package nl.rrd.senseeact.client.model.compat;

import java.util.ArrayList;
import java.util.List;

import nl.rrd.utils.json.JsonObject;

/**
 * Specification of a sensor to use in a project. It specifies a list of
 * candidate sensor products. If there is more than one candidate, then the user
 * can choose one of the sensor products. Each sensor should have a unique ID
 * within a project.
 *
 * <p>It may also specify a label ID. That is the ID of the string resource for
 * the label to present this sensor in the user interface. If you set it to
 * null, the user interface will show the name of the first candidate
 * product.</p>
 *
 * @author Dennis Hofs (RRD)
 */
public class ProjectSensorV2 extends JsonObject {
	private String id;
	private String labelId;
	private List<String> candidateProducts = new ArrayList<>();

	/**
	 * Constructs a new empty sensor specification.
	 */
	public ProjectSensorV2() {
	}

	/**
	 * Constructs a new sensor specification with one sensor product candidate.
	 *
	 * @param id the sensor ID
	 * @param labelId the label ID or null
	 * @param product the sensor product
	 */
	public ProjectSensorV2(String id, String labelId, String product) {
		this.id = id;
		this.labelId = labelId;
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
	 * Returns the ID of the string resource for the label to present this
	 * sensor in the user interface. If you set it to null, the user interface
	 * will show the name of the first candidate product.
	 *
	 * @return the label ID or null
	 */
	public String getLabelId() {
		return labelId;
	}

	/**
	 * Sets the ID of the string resource for the label to present this sensor
	 * in the user interface. If you set it to null, the user interface will
	 * show the name of the first candidate product.
	 *
	 * @param labelId the label ID or null
	 */
	public void setLabelId(String labelId) {
		this.labelId = labelId;
	}

	/**
	 * Returns the sensor product candidates. There should be at least one
	 * candidate.
	 *
	 * @return the sensor product candidates
	 */
	public List<String> getCandidateProducts() {
		return candidateProducts;
	}

	/**
	 * Sets the sensor product candidates. There should be at least one
	 * candidate.
	 *
	 * @param candidateProducts the sensor product candidates
	 */
	public void setCandidateProducts(List<String> candidateProducts) {
		this.candidateProducts = candidateProducts;
	}

	/**
	 * Adds a sensor product candidate. There should be at least one candidate.
	 *
	 * @param product the sensor product candidate
	 */
	public void addCandidateProduct(String product) {
		candidateProducts.add(product);
	}
}
