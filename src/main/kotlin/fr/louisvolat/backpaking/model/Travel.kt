package fr.louisvolat.backpaking.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "travels")
class Travel(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    var name: String,
    var description: String,
    var startDate: LocalDateTime,
    var endDate: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_cover_picture")
    var coverPicture: Picture? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_user", nullable = false)
    var user: User,

    @OneToMany(mappedBy = "travel", cascade = [CascadeType.ALL], orphanRemoval = true)
    var coordinates: MutableList<Coordinate> = mutableListOf(),

    @OneToMany(mappedBy = "travel", cascade = [CascadeType.ALL], orphanRemoval = true)
    var travelPictures: MutableList<Picture> = mutableListOf(),

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)