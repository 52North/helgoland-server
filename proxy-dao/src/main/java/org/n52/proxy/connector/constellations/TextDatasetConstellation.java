package org.n52.proxy.connector.constellations;

import org.n52.proxy.connector.utils.EntityBuilder;
import org.n52.proxy.db.beans.ProxyServiceEntity;
import org.n52.series.db.beans.CategoryEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.TextDatasetEntity;

import java.util.Date;

/**
 * @author Jan Schulte
 */
public class TextDatasetConstellation extends DatasetConstellation {

    public TextDatasetConstellation(String procedure, String offering, String category, String phenomenon,
            String feature) {
        super(procedure, offering, category, phenomenon, feature);
    }

    @Override
    public DatasetEntity createDatasetEntity(ProcedureEntity procedureEntity, CategoryEntity categoryEntity,
                                             FeatureEntity featureEntity, OfferingEntity offeringEntity,
                                             PhenomenonEntity phenomenonEntity, ProxyServiceEntity proxyServiceEntity) {
        TextDatasetEntity textDataset = new TextDatasetEntity();
        EntityBuilder.updateDatasetEntity(textDataset, procedureEntity, categoryEntity, featureEntity, offeringEntity,
                phenomenonEntity, proxyServiceEntity);
        textDataset.setFirstValueAt(new Date());
        textDataset.setLastValueAt(new Date());
        return textDataset;
    }

}
