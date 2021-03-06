package com.jianglibo.vaadin.dashboard.view.boxgrouphistory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

import com.jianglibo.vaadin.dashboard.annotation.MainMenu;
import com.jianglibo.vaadin.dashboard.event.ui.DashboardEventBus;
import com.jianglibo.vaadin.dashboard.view.DboardViewUtil;
import com.jianglibo.vaadin.dashboard.view.MenuItemWrapper;
import com.jianglibo.vaadin.dashboard.view.ValoMenuItemButton;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;

@MainMenu(menuOrder = 700)
public class BoxGroupHistoryViewMenuItem implements MenuItemWrapper {

	private Component menuItem;

	private Label notificationsBadge;
	
	private int count1;
	
	private int count2;
	
	@Autowired
	public BoxGroupHistoryViewMenuItem(MessageSource messageSource) {
        this.notificationsBadge = new Label();
        this.menuItem = DboardViewUtil.buildBadgeWrapper(new ValoMenuItemButton(BoxGroupHistoryListView.VIEW_NAME, BoxGroupHistoryListView.ICON_VALUE, messageSource),
                notificationsBadge);
        DashboardEventBus.register(this);
	}
	
	public Component getMenuItem() {
		return menuItem;
	}
	
	public void updateNotificationsCount(int newCount) {
		if (newCount == 0) {
			count1 = count2;
		} else {
			count2 = newCount;
		}
		notificationsBadge.setValue(String.valueOf(count2 - count1));
		notificationsBadge.setVisible((count2 - count1) > 0);
	}

	@Override
	public void onAttach() {
		updateNotificationsCount(0);
	}
}
