package fr.louisvolat.backpaking.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "picture_versions")
class PictureVersions(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_image", nullable = false)
    var picture: Picture,

    @Column(name = "path", nullable = false, length = 255)
    var path: String,

    @Column(name = "resolution", nullable = false)
    var resolution: Byte,

    @Column(name = "version_type", nullable = false, length = 255)
    var versionType: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
