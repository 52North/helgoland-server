/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.proxy.connector.constellations;

import java.util.Date;
import org.n52.proxy.connector.utils.EntityBuilder;
import org.n52.proxy.db.beans.ProxyServiceEntity;
import org.n52.series.db.beans.CategoryEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.ProfileDatasetEntity;
import org.n52.series.db.beans.UnitEntity;

/**
 * @author Jan Schulte
 */
public class ProfileDatasetConstellation extends DatasetConstellation<ProfileDatasetEntity> {

    private String verticalParameterName = "depth";

    private UnitEntity unit;

    public ProfileDatasetConstellation(String procedure, String offering, String category, String phenomenon,
            String feature) {
        super(procedure, offering, category, phenomenon, feature);
    }

    public UnitEntity getUnit() {
        return unit;
    }

    public void setUnit(UnitEntity unit) {
        this.unit = unit;
    }

    @Override
    public DatasetEntity createDatasetEntity(ProcedureEntity procedure, CategoryEntity category, FeatureEntity feature,
            OfferingEntity offering, PhenomenonEntity phenomenon, ProxyServiceEntity service) {
        ProfileDatasetEntity dataset = new ProfileDatasetEntity();
        dataset.setDomainId(this.getDomainId());
        EntityBuilder.updateDatasetEntity(dataset, procedure, category, feature, offering, phenomenon, service);
        if (unit == null) {
            unit = EntityBuilder.createUnit("", null, service);
        }
        dataset.setUnit(unit);
        dataset.setVerticalParameterName(verticalParameterName);
        dataset.setFirstValueAt(new Date());
        dataset.setLastValueAt(new Date());
        return dataset;
    }

}
