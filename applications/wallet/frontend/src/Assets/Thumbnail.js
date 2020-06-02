import { useRef } from 'react'
import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import Link from 'next/link'

import { colors, constants, spacing } from '../Styles'

import ExpandSvg from '../Icons/expand.svg'
import SimilaritySvg from '../Icons/similarity.svg'

import Button, { VARIANTS } from '../Button'

import { encode, decode } from '../Filters/helpers'
import { formatSeconds } from './helpers'

const AssetsThumbnail = ({
  asset: {
    id,
    metadata: {
      source: { filename },
    },
    thumbnailUrl,
    videoProxyUrl,
    videoLength,
  },
}) => {
  const playerRef = useRef()
  let playerPromise

  const {
    query: { projectId, id: selectedId, query },
  } = useRouter()

  const isSelected = id === selectedId

  const queryParams = Object.entries({
    ...(isSelected ? {} : { id }),
    ...(query ? { query } : {}),
  })
    .map(([key, value]) => `${key}=${value}`)
    .join('&')

  const queryString = queryParams ? `?${queryParams}` : ''

  const filters = decode({ query })
  const simQuery = encode({
    filters: [
      ...filters,
      // {
      //   type: 'similarity',
      //   attribute: 'analysis.zvi-image-similarity',
      //   values: {
      //     hashes: [''],
      //     minScore: 0,
      //   },
      // },
    ],
  })

  const { pathname: thumbnailSrc } = new URL(thumbnailUrl)
  const { pathname: videoSrc } = videoProxyUrl ? new URL(videoProxyUrl) : {}

  const play = /* istanbul ignore next */ () => {
    playerRef.current.currentTime = 0
    playerPromise = playerRef.current.play()
  }

  const pause = /* istanbul ignore next */ async () => {
    if (!playerPromise) return

    try {
      await playerPromise
      playerRef.current.pause()
    } catch (error) {
      // do nothing
    }
  }

  return (
    <div
      css={{
        position: 'relative',
        border: isSelected
          ? constants.borders.assetSelected
          : constants.borders.assetInactive,
        width: '100%',
        height: '100%',
        ':hover': {
          border: isSelected
            ? constants.borders.assetSelected
            : constants.borders.assetHover,
          a: {
            display: 'flex',
          },
          '.videoLength': {
            display: 'none',
          },
        },
      }}
    >
      <Link
        href={`/[projectId]/visualizer${queryString}`}
        as={`/${projectId}/visualizer${queryString}`}
        passHref
      >
        <Button
          variant={VARIANTS.NEUTRAL}
          css={{
            width: '100%',
            height: '100%',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            background: colors.structure.mattGrey,
            overflow: 'hidden',
          }}
        >
          {videoSrc ? (
            <video
              ref={playerRef}
              preload="none"
              onMouseOver={play}
              onMouseOut={pause}
              onFocus={play}
              onBlur={pause}
              css={{ width: '100%', height: '100%', objectFit: 'contain' }}
              muted
              playsInline
              controlsList="nodownload nofullscreen noremoteplayback"
              disablePictureInPicture
              poster={thumbnailSrc}
            >
              <source src={videoSrc} type="video/mp4" />
            </video>
          ) : (
            <img
              css={{ width: '100%', height: '100%', objectFit: 'contain' }}
              src={thumbnailSrc}
              alt={filename}
            />
          )}
        </Button>
      </Link>
      <Link
        href={`/[projectId]/visualizer?id=${id}&query=${simQuery}`}
        as={`/${projectId}/visualizer?id=${id}&query=${simQuery}`}
        passHref
      >
        <Button
          variant={VARIANTS.NEUTRAL}
          style={{
            display: 'none',
            position: 'absolute',
            top: spacing.small,
            right: spacing.small,
            padding: spacing.small,
            backgroundColor: colors.structure.smoke,
            opacity: constants.opacity.half,
            ':hover': {
              opacity: constants.opacity.eighth,
            },
          }}
        >
          <SimilaritySvg width={20} color={colors.structure.white} />
        </Button>
      </Link>
      <Link
        href={`/[projectId]/visualizer/[id]${queryString}`}
        as={`/${projectId}/visualizer/${id}${queryString}`}
        passHref
      >
        <Button
          variant={VARIANTS.NEUTRAL}
          style={{
            display: 'none',
            position: 'absolute',
            bottom: spacing.small,
            right: spacing.small,
            padding: spacing.small,
            backgroundColor: colors.structure.smoke,
            opacity: constants.opacity.half,
            ':hover': {
              opacity: constants.opacity.eighth,
            },
          }}
        >
          <ExpandSvg width={20} color={colors.structure.white} />
        </Button>
      </Link>
      {videoLength > 0 && (
        <div
          className="videoLength"
          css={{
            position: 'absolute',
            bottom: spacing.small,
            left: spacing.small,
            padding: spacing.mini,
            color: colors.structure.black,
            // Append 80 for half opacity without affecting text
            backgroundColor: `${colors.structure.white}80`,
          }}
        >
          {formatSeconds({ seconds: videoLength })}
        </div>
      )}
    </div>
  )
}

AssetsThumbnail.propTypes = {
  asset: PropTypes.shape({
    id: PropTypes.string.isRequired,
    metadata: PropTypes.shape({
      source: PropTypes.shape({
        path: PropTypes.string,
        filename: PropTypes.string,
        extension: PropTypes.string,
        mimetype: PropTypes.string,
      }),
    }),
    thumbnailUrl: PropTypes.string.isRequired,
    assetStyle: PropTypes.oneOf(['image', 'video', 'document']),
    videoLength: PropTypes.number,
    videoProxyUrl: PropTypes.string,
  }).isRequired,
}

export default AssetsThumbnail
