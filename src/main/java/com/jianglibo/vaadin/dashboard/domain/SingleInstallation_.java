package com.jianglibo.vaadin.dashboard.domain;

import javax.annotation.Generated;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2016-08-10T15:20:13.631+0800")
@StaticMetamodel(SingleInstallation.class)
public class SingleInstallation_ extends BaseEntity_ {
	public static volatile SingularAttribute<SingleInstallation, Software> software;
	public static volatile SetAttribute<SingleInstallation, Box> boxes;
	public static volatile SingularAttribute<SingleInstallation, Boolean> success;
	public static volatile SingularAttribute<SingleInstallation, String> config;
}