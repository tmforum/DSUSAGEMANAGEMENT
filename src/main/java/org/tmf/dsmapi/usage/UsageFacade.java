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
import org.tmf.dsmapi.usage.model.RatedProductUsage;
import org.tmf.dsmapi.usage.model.RelatedParty;
import org.tmf.dsmapi.usage.model.Status;
import org.tmf.dsmapi.usage.model.UsageCharacteristic;

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

    public void checkCreation(Usage newUsage) throws BadUsageException {
        //verify status
        if (null == newUsage.getStatus() || newUsage.getStatus().name().equalsIgnoreCase("")) {
            newUsage.setStatus(Status.Received);
        } else {
            checkStatus(newUsage);
        }

        if (null == newUsage.getDate()) {
            throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS, "date is mandatory");
        }

        if (null == newUsage.getType()) {
            throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS, "type is mandatory");
        }

    }

    public Usage updateAttributs(long id, Usage partialUsage) throws UnknownResourceException, BadUsageException {
        Usage currentProduct = this.find(id);

        if (currentProduct == null) {
            throw new UnknownResourceException(ExceptionType.UNKNOWN_RESOURCE);
        }

        if (null != partialUsage.getId()) {
            throw new BadUsageException(ExceptionType.BAD_USAGE_OPERATOR,
                    "id is not patchable");
        }

        if (null != partialUsage.getHref()) {
            throw new BadUsageException(ExceptionType.BAD_USAGE_OPERATOR,
                    "href is not patchable");
        }
        
        checkStatus(partialUsage);
        
        checkRules(partialUsage);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.convertValue(partialUsage, JsonNode.class);
        if (null != partialUsage.getStatus()) {
            stateModel.checkTransition(currentProduct.getStatus(), partialUsage.getStatus());
            publisher.statusChangedNotification(currentProduct, new Date());
//        } else {
//            throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS, "state" + " is not found");
        }

        partialUsage.setId(id);
        if (BeanUtils.patch(currentProduct, partialUsage, node)) {
            publisher.updateNotification(currentProduct, new Date());
        }

        return currentProduct;
    }

    public void checkRules(Usage usage) throws BadUsageException {
        if (null != usage.getUsageCharacteristic()) {
            for (UsageCharacteristic usageCharacteristic : usage.getUsageCharacteristic()) {
                if (null == usageCharacteristic.getName()) {
                    throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS,
                            "usageCharacteristic.name is mandatory");
                }
                if (null == usageCharacteristic.getValue()) {
                    throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS,
                            "usageCharacteristic.value is mandatory");
                }
            }
        }

        if (null != usage.getRelatedParty()) {
            for (RelatedParty relatedParty : usage.getRelatedParty()) {
                if (null == relatedParty.getRole()) {
                    throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS,
                            "relatedParty.role is mandatory");
                }
            }
        }

    }

    public void checkStatus(Usage usage) throws BadUsageException {
        if (usage.getStatus() == Status.Rated || usage.getStatus() == Status.Billed) {
            if (null == usage.getRatedProductUsage()) {
                throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS,
                        "ratedProductUsage is mandatory if status is 'rated' or 'billed'");
            } else {
                for (RatedProductUsage rpu : usage.getRatedProductUsage()) {
                    if (null == rpu.getRatingDate()) {
                        throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS,
                                "ratedProductUsage.ratingDate is mandatory if status is 'rated' or 'billed'");
                    }
                    if (null == rpu.getTaxIncludedRatingAmount()) {
                        throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS,
                                "ratedProductUsage.taxIncludedRatingAmount is mandatory if status is 'rated' or 'billed'");
                    }
                    if (null == rpu.getTaxExcludedRatingAmount()) {
                        throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS,
                                "ratedProductUsage.taxExcludedRatingAmount is mandatory if status is 'rated' or 'billed'");
                    }
                    if (null == rpu.getTaxRate()) {
                        throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS,
                                "ratedProductUsage.taxRate is mandatory if status is 'rated' or 'billed'");
                    }
                    if (null == rpu.getCurrencyCode()) {
                        throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS,
                                "ratedProductUsage.currencyCode is mandatory if status is 'rated' or 'billed'");
                    }
                    if (null == rpu.getProductRef()) {
                        throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS,
                                "ratedProductUsage.productRef is mandatory if status is 'rated' or 'billed'");
                    }
                    if (null == rpu.getUsageRatingTag()) {
                        rpu.setUsageRatingTag("Usage");
                    }
                    if (null == rpu.isIsBilled()) {
                        rpu.setIsBilled(Boolean.FALSE);
                    }
                    if (null == rpu.getRatingAmountType()) {
                        rpu.setRatingAmountType("Total");
                    }
                    if (null == rpu.isIsTaxExempt()) {
                        rpu.setIsTaxExempt(Boolean.FALSE);
                    }
                    if (null == rpu.getOfferTariffType()) {
                        rpu.setOfferTariffType("Normal");
                    }
                }
            }
        }
    }
}
