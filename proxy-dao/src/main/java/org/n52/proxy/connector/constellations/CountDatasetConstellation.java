/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.proxy.connector.constellations;

import org.n52.proxy.connector.utils.EntityBuilder;
import org.n52.proxy.db.beans.ProxyServiceEntity;
import org.n52.series.db.beans.CategoryEntity;
import org.n52.series.db.beans.CountDatasetEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.ProcedureEntity;

import java.util.Date;

/**
 * @author Jan Schulte
 */
public class CountDatasetConstellation extends DatasetConstellation {

    public CountDatasetConstellation(String procedure, String offering, String category, String phenomenon,
            String feature) {
        super(procedure, offering, category, phenomenon, feature);
    }

    @Override
    public DatasetEntity createDatasetEntity(ProcedureEntity procedureEntity, CategoryEntity categoryEntity,
                                             FeatureEntity featureEntity, OfferingEntity offeringEntity,
                                             PhenomenonEntity phenomenonEntity, ProxyServiceEntity proxyServiceEntity) {
        CountDatasetEntity countDataset = new CountDatasetEntity();
        EntityBuilder.updateDatasetEntity(countDataset, procedureEntity, categoryEntity, featureEntity, offeringEntity,
                phenomenonEntity, proxyServiceEntity);
        countDataset.setFirstValueAt(new Date());
        countDataset.setLastValueAt(new Date());
        return countDataset;
    }

}
