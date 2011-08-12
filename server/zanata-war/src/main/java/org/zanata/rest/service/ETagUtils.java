package org.zanata.rest.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.EntityTag;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.zanata.common.LocaleId;
import org.zanata.dao.DocumentDAO;
import org.zanata.model.HDocument;
import org.zanata.model.HLocale;
import org.zanata.model.HProjectIteration;
import org.zanata.model.po.HPoHeader;
import org.zanata.rest.NoSuchEntityException;
import org.zanata.rest.dto.extensions.gettext.PoHeader;
import org.zanata.util.HashUtil;

@Name("eTagUtils")
@Scope(ScopeType.STATELESS)
@AutoCreate
public class ETagUtils
{

   @In
   private Session session;

   @In
   private DocumentDAO documentDAO;

   Log log = Logging.getLog(ETagUtils.class);

   public ETagUtils()
   {
   }

   public ETagUtils(Session session, DocumentDAO documentDAO)
   {
      this.session = session;
      this.documentDAO = documentDAO;
   }

   /**
    * Retrieves the ETag for the Project
    * 
    * This algorithm takes into account changes in Project Iterations as well.
    * 
    * @param slug Project slug
    * @return calculated EntityTag or null if project does not exist
    */
   public EntityTag generateTagForProject(String slug)
   {
      Integer projectVersion = (Integer) session.createQuery("select p.versionNum from HProject p where slug =:slug").setParameter("slug", slug).uniqueResult();

      if (projectVersion == null)
         throw new NoSuchEntityException("Project '" + slug + "' not found.");
      ;

      @SuppressWarnings("unchecked")
      List<Integer> iterationVersions = session.createQuery("select i.versionNum from HProjectIteration i where i.project.slug =:slug").setParameter("slug", slug).list();

      String hash = HashUtil.generateHash(projectVersion + ':' + StringUtils.join(iterationVersions, ':'));

      return EntityTag.valueOf(hash);
   }

   /**
    * Retrieves the ETag for the ProjectIteration
    * 
    * @param projectSlug project slug
    * @param iterationSlug iteration slug
    * @return calculated EntityTag or null if iteration does not exist
    */
   public EntityTag generateETagForIteration(String projectSlug, String iterationSlug)
   {
      Integer iterationVersion = (Integer) session.createQuery("select i.versionNum from HProjectIteration i where i.slug =:islug and i.project.slug =:pslug").setParameter("islug", iterationSlug).setParameter("pslug", projectSlug).uniqueResult();

      if (iterationVersion == null)
         throw new NoSuchEntityException("Project Iteration '" + iterationSlug + "' not found.");
      ;

      String hash = HashUtil.generateHash(String.valueOf(iterationVersion));

      return EntityTag.valueOf(hash);
   }

   public EntityTag generateETagForDocument(HProjectIteration iteration, String id, Set<String> extensions)
   {
      HDocument doc = documentDAO.getByDocId(iteration, id);
      if (doc == null)
         throw new NoSuchEntityException("Document '" + id + "' not found.");
      ;
      Integer hashcode = 1;
      hashcode = hashcode * 31 + doc.getRevision();

      int extHash = 0;
      if (extensions.contains(PoHeader.ID))
      {
         HPoHeader header = doc.getPoHeader();
         if (header != null)
         {
            extHash = header.getVersionNum();
         }
      }
      hashcode = hashcode * 31 + extHash;

      return EntityTag.valueOf(String.valueOf(hashcode));
   }
   
   public EntityTag generateTagForGlossary(List<Integer> glossaryEntryIds)
   {
      List<String> glossaryVersions = new ArrayList<String>();
      for (int glossaryEntryId : glossaryEntryIds)
      {
         Integer glossaryId = (Integer) session.createQuery("select g.id from HGlossaryEntry g where id =:id").setParameter("id", glossaryEntryId).uniqueResult();
         if (glossaryId == null)
         {
            throw new NoSuchEntityException("GlossaryEntry '" + glossaryEntryId + "' not found.");
         }
         glossaryVersions.add(glossaryId.toString());
      }
      String hash = HashUtil.generateHash(StringUtils.join(glossaryVersions, ':'));
      return EntityTag.valueOf(hash);
   }

   public EntityTag generateTagForGlossaryTerm(LocaleId locale)
   {
      Object[] queryResult = (Object[]) session.createQuery("select g.glossaryEntryId,g.localeId from HGlossaryTerm g where locale =:locale").setParameter("locale", new HLocale(locale)).uniqueResult();
      if (queryResult == null)
      {
         throw new NoSuchEntityException("HGlossaryTerm with locale '" + locale + "' not found.");
      }
      String hash = HashUtil.generateHash(String.valueOf(queryResult));
      return EntityTag.valueOf(hash);
   }

}
