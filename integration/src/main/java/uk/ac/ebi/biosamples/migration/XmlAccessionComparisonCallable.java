package uk.ac.ebi.biosamples.migration;

import com.google.common.collect.Sets;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import uk.ac.ebi.biosamples.model.*;
import uk.ac.ebi.biosamples.service.XmlGroupToSampleConverter;
import uk.ac.ebi.biosamples.service.XmlSampleToSampleConverter;
import uk.ac.ebi.biosamples.utils.ThreadUtils;
import uk.ac.ebi.biosamples.utils.XmlPathBuilder;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

class XmlAccessionComparisonCallable implements Callable<Void> {
	private final RestTemplate restTemplate;
	private final String oldUrl;
	private final String newUrl;
	private final Queue<String> bothQueue;
	private final AtomicBoolean bothFlag;
	private final XmlSampleToSampleConverter xmlSampleToSampleConverter;
	private final XmlGroupToSampleConverter xmlGroupToSampleConverter;
	private final ExecutorService executorService = Executors.newFixedThreadPool(8);
	private final boolean compare;

	private final Logger log = LoggerFactory.getLogger(getClass());

	public XmlAccessionComparisonCallable(RestTemplate restTemplate, String oldUrl, String newUrl, Queue<String> bothQueue,
			AtomicBoolean bothFlag, 
			XmlSampleToSampleConverter xmlSampleToSampleConverter, XmlGroupToSampleConverter xmlGroupToSampleConverter, 
			boolean compare) {
		this.restTemplate = restTemplate;
		this.oldUrl = oldUrl;
		this.newUrl = newUrl;
		this.bothQueue = bothQueue;
		this.bothFlag = bothFlag;
		this.xmlSampleToSampleConverter = xmlSampleToSampleConverter;
		this.xmlGroupToSampleConverter = xmlGroupToSampleConverter;
		this.compare = compare;
	}

	@Override
	public Void call() throws Exception {
		log.info("Started");
		log.info("oldUrl = "+oldUrl);
		log.info("newUrl = "+newUrl);
		log.info("compare = "+compare);
		Map<String, Future<Void>> futures = new LinkedHashMap<>();

		while (!bothFlag.get() || !bothQueue.isEmpty()) {
			String accession = bothQueue.poll();
			if (accession != null) {
				log.trace("Comparing accession "+ accession);
				if (compare) {
					futures.put(accession, executorService.submit(new XmlCompareCallable(accession, oldUrl, newUrl, 
							xmlSampleToSampleConverter, xmlGroupToSampleConverter,
							restTemplate)));
					ThreadUtils.checkFutures(futures, 1000);
				}
			} else {
				Thread.sleep(100);
			}
		}
		ThreadUtils.checkFutures(futures, 0);
		executorService.awaitTermination(1, TimeUnit.MINUTES);
		log.info("Finished AccessionComparisonCallable.call(");
		return null;
	}
}