package cloudservice;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import java.util.Set;

@Path("cluster")
public class ClusterDefinitionResource {
	private final Cluster cluster;

	@Inject
	public ClusterDefinitionResource(Cluster cluster) {
		this.cluster = cluster;
	}

	@POST
	public void setCluster(@QueryParam("thisInstance") String thisInstance, Set<String> allInstances) {
		if (thisInstance == null || thisInstance.isBlank()) {
			throw new BadRequestException("Missing 'thisInstance' argument");
		}
		if (allInstances == null || allInstances.isEmpty()) {
			throw new BadRequestException("Missing 'allInstances' argument");
		}
		if (!allInstances.contains(thisInstance)) {
			throw new BadRequestException("'thisInstance' is missing from 'allInstances' argument");
		}

		cluster.setAllInstances(thisInstance, allInstances);
	}

	@GET
	public Set<String> list() {
		return cluster.getAllInstances();
	}
}
