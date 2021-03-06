package com.jianglibo.vaadin.dashboard.event.ui;

/*
 * Event bus events used in Dashboard are listed here as inner classes.
 */
public abstract class DashboardEvent {
	
	public static final class SoftwareNumberChangeEvent {
		
		private final int number;
		
		public SoftwareNumberChangeEvent() {
			super();
			this.number = 1;
		}


		public SoftwareNumberChangeEvent(int number) {
			super();
			this.number = number;
		}

		public int getNumber() {
			return number;
		}
		
	}

    public static final class UserLoginRequestedEvent {
        private final String userName, password;

        public UserLoginRequestedEvent(final String userName,
                final String password) {
            this.userName = userName;
            this.password = password;
        }

        public String getUserName() {
            return userName;
        }

        public String getPassword() {
            return password;
        }
    }

    public static class BrowserResizeEvent {

    }

    public static class UserLoggedOutEvent {

    }

    public static class NotificationsCountUpdatedEvent {
    }

    public static final class ReportsCountUpdatedEvent {
        private final int count;

        public ReportsCountUpdatedEvent(final int count) {
            this.count = count;
        }

        public int getCount() {
            return count;
        }

    }

    public static final class PostViewChangeEvent {
        private final String viewName;

        public PostViewChangeEvent(final String viewName) {
            this.viewName = viewName;
        }

        public String getViewName() {
            return viewName;
        }
    }
    
    public static class CloseOpenWindowsEvent {
    }

    public static class ProfileUpdatedEvent {
    }

}
