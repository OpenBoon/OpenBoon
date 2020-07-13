import { useRef } from 'react'
import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import Link from 'next/link'

import { colors, constants, spacing } from '../Styles'

import ExpandSvg from '../Icons/expand.svg'
import SimilaritySvg from '../Icons/similarity.svg'

import Button, { VARIANTS } from '../Button'

import { dispatch, ACTIONS } from '../Filters/helpers'
import { formatSeconds } from './helpers'

const ICON_SIZE = 20

const AssetsThumbnail = ({
  asset: {
    id,
    metadata: { source },
    thumbnailUrl,
    videoProxyUrl,
    videoLength,
  },
}) => {
  const { filename } = source || {}

  const playerRef = useRef()

  const {
    pathname,
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

  const { pathname: thumbnailSrc } = new URL(thumbnailUrl)
  const { pathname: videoSrc } = videoProxyUrl ? new URL(videoProxyUrl) : {}

  return (
    <div
      css={{
        position: 'relative',
        border: isSelected
          ? constants.borders.assetSelected
          : constants.borders.large.transparent,
        width: '100%',
        height: '100%',
        ':hover': {
          border: isSelected
            ? constants.borders.assetSelected
            : constants.borders.large.white,
          'a, button': {
            opacity: 1,
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
          style={{ opacity: 1 }}
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

      <Button
        aria-label="Find similar images"
        variant={VARIANTS.NEUTRAL}
        style={{
          opacity: 0,
          position: 'absolute',
          top: spacing.small,
          right: spacing.small,
          padding: spacing.small,
          backgroundColor: `${colors.structure.smoke}${constants.opacity.hexHalf}`,
          ':hover,  &.focus-visible:focus': {
            opacity: 1,
            backgroundColor: colors.structure.smoke,
          },
        }}
        onClick={() => {
          dispatch({
            type: ACTIONS.APPLY_SIMILARITY,
            payload: {
              pathname,
              projectId,
              assetId: id,
              selectedId,
              query,
            },
          })
        }}
      >
        <SimilaritySvg height={ICON_SIZE} color={colors.structure.white} />
      </Button>

      <Link
        href={`/[projectId]/visualizer/[id]${queryString}`}
        as={`/${projectId}/visualizer/${id}${queryString}`}
        passHref
      >
        <Button
          variant={VARIANTS.NEUTRAL}
          style={{
            opacity: 0,
            position: 'absolute',
            bottom: spacing.small,
            right: spacing.small,
            padding: spacing.small,
            backgroundColor: `${colors.structure.smoke}${constants.opacity.hexHalf}`,
            ':hover,  &.focus-visible:focus': {
              opacity: 1,
              backgroundColor: colors.structure.smoke,
            },
          }}
        >
          <ExpandSvg height={ICON_SIZE} color={colors.structure.white} />
        </Button>
      </Link>

      {videoLength > 0 && (
        <div
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
    }).isRequired,
    thumbnailUrl: PropTypes.string.isRequired,
    assetStyle: PropTypes.oneOf(['image', 'video', 'document']),
    videoLength: PropTypes.number,
    videoProxyUrl: PropTypes.string,
  }).isRequired,
}

export default AssetsThumbnail
