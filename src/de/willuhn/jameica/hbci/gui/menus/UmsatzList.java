/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus/src/de/willuhn/jameica/hbci/gui/menus/UmsatzList.java,v $
 * $Revision: 1.38 $
 * $Date: 2011/05/06 09:03:54 $
 * $Author: willuhn $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by willuhn.webdesign
 * All rights reserved
 *
 **********************************************************************/
package de.willuhn.jameica.hbci.gui.menus;

import java.rmi.RemoteException;

import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.extension.Extendable;
import de.willuhn.jameica.gui.extension.ExtensionRegistry;
import de.willuhn.jameica.gui.internal.action.Print;
import de.willuhn.jameica.gui.parts.CheckedContextMenuItem;
import de.willuhn.jameica.gui.parts.CheckedSingleContextMenuItem;
import de.willuhn.jameica.gui.parts.ContextMenu;
import de.willuhn.jameica.gui.parts.ContextMenuItem;
import de.willuhn.jameica.hbci.HBCI;
import de.willuhn.jameica.hbci.Settings;
import de.willuhn.jameica.hbci.gui.action.AuslandsUeberweisungNew;
import de.willuhn.jameica.hbci.gui.action.DBObjectDelete;
import de.willuhn.jameica.hbci.gui.action.EmpfaengerAdd;
import de.willuhn.jameica.hbci.gui.action.FlaggableChange;
import de.willuhn.jameica.hbci.gui.action.UmsatzAssign;
import de.willuhn.jameica.hbci.gui.action.UmsatzDetail;
import de.willuhn.jameica.hbci.gui.action.UmsatzExport;
import de.willuhn.jameica.hbci.gui.action.UmsatzImport;
import de.willuhn.jameica.hbci.gui.action.UmsatzMarkChecked;
import de.willuhn.jameica.hbci.gui.action.UmsatzTypNew;
import de.willuhn.jameica.hbci.io.print.PrintSupportUmsatzList;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.hbci.rmi.Umsatz;
import de.willuhn.jameica.hbci.rmi.UmsatzTyp;
import de.willuhn.jameica.hbci.server.UmsatzTreeNode;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;

/**
 * Liefert ein vorgefertigtes Kontext-Menu, welches an Listen von Umsaetzen
 * angehaengt werden kann.
 */
public class UmsatzList extends ContextMenu implements Extendable
{

	private final static I18N i18n = Application.getPluginLoader().getPlugin(HBCI.class).getResources().getI18N();

  /**
   * Erzeugt ein Kontext-Menu fuer eine Liste von Umsaetzen.
   */
  public UmsatzList()
  {
    this(null);
  }

  /**
	 * Erzeugt ein Kontext-Menu fuer eine Liste von Umsaetzen.
   * @param konto optionale Angabe des Kontos.
	 */
	public UmsatzList(final Konto konto)
	{
		addItem(new OpenItem());
    addItem(new UmsatzItem(i18n.tr("L�schen..."), new DBObjectDelete(),"user-trash-full.png"));
    addItem(ContextMenuItem.SEPARATOR);
    addItem(new UmsatzItem(i18n.tr("In Adressbuch �bernehmen"),new EmpfaengerAdd(),"contact-new.png"));
    addItem(new UmsatzItem(i18n.tr("Als neue �berweisung anlegen..."),new AuslandsUeberweisungNew(),"stock_next.png"));
    addItem(ContextMenuItem.SEPARATOR);
    addItem(new UmsatzBookedItem(i18n.tr("als \"gepr�ft\" markieren..."),new UmsatzMarkChecked(Umsatz.FLAG_CHECKED,true),"emblem-default.png","ALT+G"));
    addItem(new UmsatzBookedItem(i18n.tr("als \"ungepr�ft\" markieren..."),new FlaggableChange(Umsatz.FLAG_CHECKED,false),"edit-undo.png","CTRL+ALT+G"));
    addItem(ContextMenuItem.SEPARATOR);
    addItem(new UmsatzItem(i18n.tr("Drucken..."),new Action() {
      public void handleAction(Object context) throws ApplicationException
      {
        new Print().handleAction(new PrintSupportUmsatzList(context));
      }
    },"document-print.png"));
    addItem(new UmsatzOrGroupItem(i18n.tr("Exportieren..."),new UmsatzExport(),"document-save.png"));
    addItem(new ContextMenuItem(i18n.tr("Importieren..."),new UmsatzImport()
    {

      public void handleAction(Object context) throws ApplicationException
      {
        super.handleAction(konto != null ? konto : context);
      }
      
    }
    ,"document-open.png"));
    
    // BUGZILLA 512 / 1115
    addItem(ContextMenuItem.SEPARATOR);
    addItem(new UmsatzBookedItem(i18n.tr("Kategorie zuordnen..."),new UmsatzAssign(),"x-office-spreadsheet.png","ALT+K"));
    addItem(new CheckedSingleContextMenuItem(i18n.tr("Kategorie bearbeiten..."),new UmsatzTypNew(),"document-open.png")
    {
      public boolean isEnabledFor(Object o)
      {
        // Wen es ein Umsatz ist, dann nur aktivieren, wenn der Umsatz eine Kategorie hat
        if (o instanceof Umsatz)
        {
          try
          {
            return ((Umsatz)o).getUmsatzTyp() != null;
          }
          catch (RemoteException re)
          {
            Logger.error("unable to check umsatztyp",re);
          }
        }
        
        // Ansonsten wie gehabt
        return super.isEnabledFor(o);
      }
      
    });
    addItem(new ContextMenuItem(i18n.tr("Neue Kategorie anlegen..."),new Action()
    {
      public void handleAction(Object context) throws ApplicationException
      {
        // BUGZILLA 926
        UmsatzTyp ut = null;
        if (context != null)
        {
          try
          {
            if (context instanceof Umsatz)
            {
              Umsatz u = (Umsatz) context;
              ut = (UmsatzTyp) Settings.getDBService().createObject(UmsatzTyp.class,null);
              ut.setName(u.getGegenkontoName());
              ut.setPattern(u.getZweck());
            }
            else if (context instanceof UmsatzTyp)
            {
              ut = (UmsatzTyp) Settings.getDBService().createObject(UmsatzTyp.class,null);
              ut.setParent((UmsatzTyp) context);
            }
            else if (context instanceof UmsatzTreeNode)
            {
              ut = (UmsatzTyp) Settings.getDBService().createObject(UmsatzTyp.class,null);
              ut.setParent(((UmsatzTreeNode) context).getUmsatzTyp());
            }
          }
          catch (Exception e)
          {
            Logger.error("error while preparing category",e);
          }
        }
        new UmsatzTypNew().handleAction(ut);
      }
    },"text-x-generic.png"));

    // Wir geben das Context-Menu jetzt noch zur Erweiterung frei.
    ExtensionRegistry.extend(this);

	}
	
  /**
   * Pruefen, ob es sich wirklich um einen Umsatz handelt.
   */
  private class UmsatzItem extends CheckedContextMenuItem
  {
    /**
     * ct.
     * @param text Label.
     * @param action Action.
     * @param icon optionales Icon.
     */
    public UmsatzItem(String text, Action action, String icon)
    {
      super(text,action,icon);
    }

    /**
     * @see de.willuhn.jameica.gui.parts.CheckedContextMenuItem#isEnabledFor(java.lang.Object)
     */
    public boolean isEnabledFor(Object o)
    {
      if ((o instanceof Umsatz) || (o instanceof Umsatz[]))
        return super.isEnabledFor(o);
      return false;
    }
  }
  
  /**
   * Akzeptiert Umsaetze oder eine einzelne Umsatzgruppe.
   * Die Gruppe allerdings nur, wenn sie direkt Umsaetze enthaelt.
   * Indirekte Umsaetze ueber Unterkategorien sind nicht moeglich.
   */
  private class UmsatzOrGroupItem extends CheckedContextMenuItem
  {
    /**
     * ct.
     * @param text Label.
     * @param action Action.
     * @param icon optionales Icon.
     */
    public UmsatzOrGroupItem(String text, Action action, String icon)
    {
      super(text,action,icon);
    }

    /**
     * @see de.willuhn.jameica.gui.parts.CheckedContextMenuItem#isEnabledFor(java.lang.Object)
     */
    public boolean isEnabledFor(Object o)
    {
      if ((o instanceof Umsatz) || (o instanceof Umsatz[]))
        return super.isEnabledFor(o);
      
      if (o instanceof UmsatzTreeNode)
      {
        UmsatzTreeNode node = (UmsatzTreeNode) o;
        return node.getUmsaetze().size() > 0 && super.isEnabledFor(o);
      }
      if (o instanceof UmsatzTreeNode[])
      {
        for (UmsatzTreeNode node:(UmsatzTreeNode[])o)
        {
          if (node.getUmsaetze().size() > 0)
            return super.isEnabledFor(o);
        }
        return false;
      }
      return false;
    }
  }

  /**
   * Ueberschrieben, um zu pruefen, ob ein Array oder ein einzelnes Element markiert ist.
   */
  private class OpenItem extends UmsatzItem
  {
    private OpenItem()
    {
      super(i18n.tr("�ffnen"),new UmsatzDetail(),"document-open.png");
    }
    /**
     * @see de.willuhn.jameica.gui.parts.ContextMenuItem#isEnabledFor(java.lang.Object)
     */
    public boolean isEnabledFor(Object o)
    {
      if (o instanceof Umsatz)
        return super.isEnabledFor(o);
      return false;
    }
  }

  /**
   * @see de.willuhn.jameica.gui.extension.Extendable#getExtendableID()
   */
  public String getExtendableID()
  {
    return this.getClass().getName();
  }

  /**
   * Ueberschrieben, um nur fuer gebuchte Umsaetze zu aktivieren
   */
  private class UmsatzBookedItem extends UmsatzItem
  {
    /**
     * ct.
     * @param text Label.
     * @param action Action.
     * @param icon optionales Icon.
     * @param shortcut Shortcut.
     */
    public UmsatzBookedItem(String text, Action action, String icon, String shortcut)
    {
      super(text,action,icon);
      this.setShortcut(shortcut);
    }
    
    /**
     * @see de.willuhn.jameica.gui.parts.ContextMenuItem#isEnabledFor(java.lang.Object)
     */
    public boolean isEnabledFor(Object o)
    {
      if ((o instanceof Umsatz) || (o instanceof Umsatz[]))
      {
        Umsatz[] umsaetze = null;
        
        if (o instanceof Umsatz)
          umsaetze = new Umsatz[]{(Umsatz) o};
        else
          umsaetze = (Umsatz[]) o;

        try
        {
          for (int i=0;i<umsaetze.length;++i)
          {
            if ((umsaetze[i].getFlags() & Umsatz.FLAG_NOTBOOKED) != 0)
              return false;
          }
        }
        catch (RemoteException re)
        {
          Logger.error("unable to check for not-booked entries",re);
        }
        return super.isEnabledFor(o);
      }
      return false;
    }
  }
  

}
