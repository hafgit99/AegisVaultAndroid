/**
 * Navigation Types for Aegis Vault Android
 */
export type RootStackParamList = {
  Auth: { isSetup: boolean };
  Dashboard: undefined;
  EntryForm: { entryId?: string };
  Settings: undefined;
  ViewEntry: { entryId: string };
  Folders: undefined;
  FolderDetail: { folderId: string };
  Security: undefined;
  Backup: undefined;
};

export type DashboardStackParamList = {
  DashboardMain: undefined;
  ViewEntry: { entryId: string };
  EditEntry: { entryId: string };
};

export declare global {
  namespace ReactNavigation {
    interfaces {
      RootStackParamList: RootStackParamList;
    }
  }
}
