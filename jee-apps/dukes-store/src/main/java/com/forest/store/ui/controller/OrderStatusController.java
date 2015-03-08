/**
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
 *
 * You may not modify, use, reproduce, or distribute this software except in
 * compliance with  the terms of the License at:
 * http://java.net/projects/javaeetutorial/pages/BerkeleyLicense
 */
package com.forest.store.ui.controller;

import java.util.ResourceBundle;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.inject.Named;

import com.forest.model.OrderStatus;
import com.forest.store.ui.util.AbstractPaginationHelper;
import com.forest.store.ui.util.JsfUtil;
import com.forest.store.ui.util.PageNavigation;
import com.forest.store.wiring.ecommerce.OrderStatusBean;

@Named(value = "orderStatusController")
@RequestScoped
public class OrderStatusController {
	private static final String BUNDLE = "bundles.Bundle";

	private OrderStatus current;
	private DataModel items = null;
	@EJB
	private com.forest.store.wiring.ecommerce.OrderStatusBean ejbFacade;
	private AbstractPaginationHelper pagination;
	private int selectedItemIndex;

	public OrderStatusController() {
	}

	public OrderStatus getSelected() {
		if (current == null) {
			current = ejbFacade.newOrderStatusInstance();
			selectedItemIndex = -1;
		}
		return current;
	}

	private OrderStatusBean getFacade() {
		return ejbFacade;
	}

	public AbstractPaginationHelper getPagination() {
		if (pagination == null) {
			pagination = new AbstractPaginationHelper(10) {

				@Override
				public int getItemsCount() {
					return getFacade().count();
				}

				@Override
				public DataModel createPageDataModel() {
					return new ListDataModel(getFacade().getAllInRange(
							getPageFirstItem(),
							getPageFirstItem() + getPageSize()));
				}
			};
		}
		return pagination;
	}

	public PageNavigation prepareList() {
		recreateModel();
		return PageNavigation.LIST;
	}

	public PageNavigation prepareView() {
		current = (OrderStatus) getItems().getRowData();
		selectedItemIndex = pagination.getPageFirstItem()
				+ getItems().getRowIndex();
		return PageNavigation.VIEW;
	}

	public PageNavigation prepareCreate() {
		current = ejbFacade.newOrderStatusInstance();
		selectedItemIndex = -1;
		return PageNavigation.CREATE;
	}

	public PageNavigation create() {
		try {
			getFacade().createOrderStatus(current);
			JsfUtil.addSuccessMessage(ResourceBundle.getBundle(BUNDLE)
					.getString("OrderStatusCreated"));
			return prepareCreate();
		} catch (Exception e) {
			JsfUtil.addErrorMessage(e, ResourceBundle.getBundle(BUNDLE)
					.getString("PersistenceErrorOccured"));
			return null;
		}
	}

	public PageNavigation prepareEdit() {
		current = (OrderStatus) getItems().getRowData();
		selectedItemIndex = pagination.getPageFirstItem()
				+ getItems().getRowIndex();
		return PageNavigation.EDIT;
	}

	public PageNavigation update() {
		try {
			getFacade().updateOrderStatus(current);
			JsfUtil.addSuccessMessage(ResourceBundle.getBundle(BUNDLE)
					.getString("OrderStatusUpdated"));
			return PageNavigation.VIEW;
		} catch (Exception e) {
			JsfUtil.addErrorMessage(e, ResourceBundle.getBundle(BUNDLE)
					.getString("PersistenceErrorOccured"));
			return null;
		}
	}

	public PageNavigation destroy() {
		current = (OrderStatus) getItems().getRowData();
		selectedItemIndex = pagination.getPageFirstItem()
				+ getItems().getRowIndex();
		performDestroy();
		recreateModel();
		return PageNavigation.LIST;
	}

	public PageNavigation destroyAndView() {
		performDestroy();
		recreateModel();
		updateCurrentItem();
		if (selectedItemIndex >= 0) {
			return PageNavigation.VIEW;
		} else {
			// all items were removed - go back to list
			recreateModel();
			return PageNavigation.LIST;
		}
	}

	private void performDestroy() {
		try {
			getFacade().removeOrderStatus(current);
			JsfUtil.addSuccessMessage(ResourceBundle.getBundle(BUNDLE)
					.getString("OrderStatusDeleted"));
		} catch (Exception e) {
			JsfUtil.addErrorMessage(e, ResourceBundle.getBundle(BUNDLE)
					.getString("PersistenceErrorOccured"));
		}
	}

	private void updateCurrentItem() {
		int count = getFacade().count();
		if (selectedItemIndex >= count) {
			// selected index cannot be bigger than number of items:
			selectedItemIndex = count - 1;
			// go to previous page if last page disappeared:
			if (pagination.getPageFirstItem() >= count) {
				pagination.previousPage();
			}
		}
		if (selectedItemIndex >= 0) {
			current = getFacade().getAllInRange(
					new int[] { selectedItemIndex, selectedItemIndex + 1 })
					.get(0);
		}
	}

	public DataModel getItems() {
		if (items == null) {
			items = getPagination().createPageDataModel();
		}
		return items;
	}

	private void recreateModel() {
		items = null;
	}

	public PageNavigation next() {
		getPagination().nextPage();
		recreateModel();
		return PageNavigation.LIST;
	}

	public PageNavigation previous() {
		getPagination().previousPage();
		recreateModel();
		return PageNavigation.LIST;
	}

	public SelectItem[] getItemsAvailableSelectMany() {
		return JsfUtil.getSelectItems(ejbFacade.getAll(), false);
	}

	public SelectItem[] getItemsAvailableSelectOne() {
		return JsfUtil.getSelectItems(ejbFacade.getAll(), true);
	}

	@FacesConverter(forClass = OrderStatus.class)
	public static class OrderStatusControllerConverter implements Converter {

		@Override
		public Object getAsObject(FacesContext facesContext,
				UIComponent component, String value) {
			if (value == null || value.length() == 0) {
				return null;
			}
			OrderStatusController controller = (OrderStatusController) facesContext
					.getApplication()
					.getELResolver()
					.getValue(facesContext.getELContext(), null,
							"orderStatusController");
			return controller.ejbFacade.getOrderStatus(getKey(value));
		}

		java.lang.Integer getKey(String value) {
			java.lang.Integer key;
			key = Integer.valueOf(value);
			return key;
		}

		String getStringKey(java.lang.Integer value) {
			StringBuilder sb = new StringBuilder();
			sb.append(value);
			return sb.toString();
		}

		@Override
		public String getAsString(FacesContext facesContext,
				UIComponent component, Object object) {
			if (object == null) {
				return null;
			}
			if (object instanceof OrderStatus) {
				OrderStatus o = (OrderStatus) object;
				return getStringKey(o.getId());
			} else {
				throw new IllegalArgumentException("object " + object
						+ " is of type " + object.getClass().getName()
						+ "; expected type: "
						+ OrderStatusController.class.getName());
			}
		}
	}
}