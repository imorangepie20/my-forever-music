package io.myforevermusic.api.modules.mainpage.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MagazineArticleCacheRepository extends JpaRepository<MagazineArticleCacheEntity, String> {

    List<MagazineArticleCacheEntity> findAllByArticleUrlIn(Collection<String> articleUrls);

    @Modifying
    @Query("delete from MagazineArticleCacheEntity entry where entry.articleUrl not in :articleUrls")
    int deleteByArticleUrlNotIn(@Param("articleUrls") Collection<String> articleUrls);
}
