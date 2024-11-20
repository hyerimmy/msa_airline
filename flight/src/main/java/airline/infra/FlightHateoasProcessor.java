package airline.infra;

import airline.domain.*;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

@Component
public class FlightHateoasProcessor
    implements RepresentationModelProcessor<EntityModel<Flight>> {

    @Override
    public EntityModel<Flight> process(EntityModel<Flight> model) {
        return model;
    }
}
