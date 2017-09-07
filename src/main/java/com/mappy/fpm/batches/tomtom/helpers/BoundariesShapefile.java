package com.mappy.fpm.batches.tomtom.helpers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mappy.fpm.batches.tomtom.TomtomShapefile;
import com.mappy.fpm.batches.tomtom.dbf.names.NameProvider;
import com.mappy.fpm.batches.utils.Feature;
import com.mappy.fpm.batches.utils.GeometrySerializer;
import com.mappy.fpm.batches.utils.LongLineSplitter;
import com.neovisionaries.i18n.CountryCode;
import com.vividsolutions.jts.algorithm.Centroid;
import com.vividsolutions.jts.geom.*;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.ImmutableMap.of;
import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.valueOf;
import static java.util.Optional.ofNullable;
import static org.openstreetmap.osmosis.core.domain.v0_6.EntityType.Node;
import static org.openstreetmap.osmosis.core.domain.v0_6.EntityType.Way;

public class BoundariesShapefile extends TomtomShapefile {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private final String adminLevel;
    private final String tomtomLevel;
    private final NameProvider nameProvider;


    protected BoundariesShapefile(String filename, int adminLevel, int tomtomLevel, NameProvider nameProvider) {
        super(filename);
        this.adminLevel = String.valueOf(adminLevel);
        this.tomtomLevel = String.valueOf(tomtomLevel);
        this.nameProvider = nameProvider;
        this.nameProvider.loadFromFile("___an.dbf", "NAME", false);
    }

    @Override
    public void serialize(GeometrySerializer serializer, Feature feature) {
        serialize(serializer, feature, Lists.newArrayList());
    }

    public void serialize(GeometrySerializer serializer, Feature feature, List<RelationMember> members) {
        String name = feature.getString("NAME");
        Long extId = feature.getLong("ID");
        String order = feature.getString("ORDER0" + tomtomLevel);
        Optional<Long> population = ofNullable(feature.getLong("POP"));

        Map<String, String> tags = nameProvider.getAlternateNames(extId);
        tags.putAll(of(
                "ref:tomtom", String.valueOf(extId),
                "ref:INSEE", CountryCode.getByCode(order) == null ? order : valueOf(CountryCode.getByCode(order).getNumeric())
        ));
        population.ifPresent(pop -> tags.put("population", valueOf(pop)));
        addRelations(serializer, feature, members, name, tags);
    }

    public void writeRelations(GeometrySerializer serializer, List<RelationMember> members, Map<String, String> tags) {
        tags.put("type", "boundary");
        serializer.writeRelation(members, tags);
    }

    public void addRelations(GeometrySerializer serializer, Feature feature, List<RelationMember> members, String name, Map<String, String> tags) {
        if (name != null) {
            Map<String, String> wayTags = newHashMap(of(
                    "name", name,
                    "boundary", "administrative",
                    "admin_level", adminLevel));
            Map<String, String> pointTags = newHashMap(tags);
            pointTags.put("name", name);
            MultiPolygon multiPolygon = feature.getMultiPolygon();
            addPointWithRoleLabel(serializer, members, pointTags, multiPolygon);
            for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);
                for (Geometry geom : LongLineSplitter.split(polygon.getExteriorRing(), 100)) {
                    Way way = serializer.write((LineString) geom, wayTags);
                    members.add(new RelationMember(way.getId(), Way, "outer"));
                }
            }
            tags.putAll(wayTags);
            writeRelations(serializer, members, tags);
        }
    }

    private void addPointWithRoleLabel(GeometrySerializer serializer, List<RelationMember> members, Map<String, String> tags, MultiPolygon multiPolygon) {
        Coordinate centPt = Centroid.getCentroid(multiPolygon);
        Optional<Node> node = serializer.writePoint(GEOMETRY_FACTORY.createPoint(centPt), tags);
        node.ifPresent(nodeLabel -> members.add(new RelationMember(nodeLabel.getId(), Node, "label")));
    }

}
