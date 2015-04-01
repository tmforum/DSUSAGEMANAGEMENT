package org.tmf.dsmapi.usage;

import java.util.Date;
import org.tmf.dsmapi.commons.facade.AbstractFacade;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.tmf.dsmapi.commons.exceptions.BadUsageException;
import org.tmf.dsmapi.commons.exceptions.ExceptionType;
import org.tmf.dsmapi.commons.exceptions.UnknownResourceException;
import org.tmf.dsmapi.commons.utils.BeanUtils;
import org.tmf.dsmapi.usage.model.Usage;
import org.tmf.dsmapi.usage.event.UsageEventPublisherLocal;

@Stateless
public class UsageFacade extends AbstractFacade<Usage> {

    @PersistenceContext(unitName = "DSUsagePU")
    private EntityManager em;
    @EJB
    UsageEventPublisherLocal publisher;

    StateModelImpl stateModel = new StateModelImpl();

    public UsageFacade() {
        super(Usage.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @Override
    public void create(Usage entity) throws BadUsageException {
        if (entity.getId() != null) {
            throw new BadUsageException(ExceptionType.BAD_USAGE_GENERIC, "While creating Usage, id must be null");
        }

        super.create(entity);
    }

    public Usage updateAttributs(long id, Usage partialUsage) throws UnknownResourceException, BadUsageException {
        Usage currentProduct = this.find(id);

        if (currentProduct == null) {
            throw new UnknownResourceException(ExceptionType.UNKNOWN_RESOURCE);
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.convertValue(partialUsage, JsonNode.class);
        if (BeanUtils.verify(partialUsage, node, "status")) {
            stateModel.checkTransition(currentProduct.getStatus(), partialUsage.getStatus());
            publisher.statusChangedNotification(currentProduct, new Date());
//        } else {
//            throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS, "state" + " is not found");
        }

        partialUsage.setId(id);
        if (BeanUtils.patch(currentProduct, partialUsage, node)) {
            publisher.valueChangedNotification(currentProduct, new Date());
        }

        return currentProduct;
    }

}
