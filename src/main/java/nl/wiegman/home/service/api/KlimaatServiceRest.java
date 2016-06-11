package nl.wiegman.home.service.api;

import nl.wiegman.home.model.Klimaat;
import nl.wiegman.home.service.KlimaatService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Component
@Produces(MediaType.APPLICATION_JSON)
@Path("klimaat")
public class KlimaatServiceRest {

    @Inject
    private KlimaatService klimaatService;

    @POST
    public Response add(Klimaat klimaat) {
        klimaatService.add(klimaat);
        return Response.status(Response.Status.CREATED).build();
    }

    @GET
    @Path("meest-recente")
    public Klimaat getMostRecent() {
        return klimaatService.getMostRecent();
    }

    @GET
    @Path("get/{from}/{to}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Klimaat> get(@PathParam("from") long from, @PathParam("to") long to) {
        return klimaatService.getInPeriod(from, to);
    }

}
