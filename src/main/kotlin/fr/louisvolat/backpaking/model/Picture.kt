package fr.louisvolat.backpaking.model

import jakarta.persistence.*
import java.time.LocalDateTime


@Entity
@Table(name = "pictures")
class Picture(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    var latitude: String,
    var longitude: String,
    var altitude: String? = null,
    var date: LocalDateTime,
    var rawVersion: String,
    var desktopVersions: String?,
    var mobileVersions: String?,
    var tabletVersions: String?,
    var iconVersions: String?,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "travel_id", nullable = false)
    var travel: Travel,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
