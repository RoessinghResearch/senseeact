package nl.rrd.senseeact.client.model.compat;

import java.util.ArrayList;
import java.util.List;

import nl.rrd.utils.json.JsonObject;
import nl.rrd.senseeact.client.sensor.BaseSensor;

/**
 * Specification of a sensor to use in a project. The sensor ID should be one
 * of the SENSOR_* constants defined in this class. This class defines a list of
 * one or more candidate sensor products.
 *
 * @author Dennis Hofs (RRD)
 */
public class ProjectSensorV3 extends JsonObject {
	private String id;
	private List<BaseSensor> candidateProducts = new ArrayList<>();

	/**
	 * Constructs a new empty sensor specification.
	 */
	public ProjectSensorV3() {
	}

	/**
	 * Constructs a new sensor specification with one sensor product candidate.
	 *
	 * @param id the sensor ID
	 * @param product the sensor product
	 */
	public ProjectSensorV3(String id, BaseSensor product) {
		this.id = id;
		candidateProducts.add(product);
	}

	/**
	 * Returns the sensor ID.
	 *
	 * @return the sensor ID
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the sensor ID.
	 *
	 * @param id the sensor ID
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Returns the sensor product candidates. There should be at least one
	 * candidate.
	 *
	 * @return the sensor product candidates
	 */
	public List<BaseSensor> getCandidateProducts() {
		return candidateProducts;
	}
	
	/**
	 * Returns whether the specified sensor product is in the list of
	 * candidates.
	 * 
	 * @param product the sensor product
	 * @return true if the product is in the list of candidates, false otherwise
	 */
	public boolean containsCandidateProduct(String product) {
		for (BaseSensor candidate : candidateProducts) {
			if (candidate.getProduct().equals(product))
				return true;
		}
		return false;
	}

	/**
	 * Sets the sensor product candidates. There should be at least one
	 * candidate.
	 *
	 * @param candidateProducts the sensor product candidates
	 */
	public void setCandidateProducts(List<BaseSensor> candidateProducts) {
		this.candidateProducts = candidateProducts;
	}

	/**
	 * Adds a sensor product candidate. There should be at least one candidate.
	 *
	 * @param product the sensor product candidate
	 */
	public void addCandidateProduct(BaseSensor product) {
		candidateProducts.add(product);
	}
}
