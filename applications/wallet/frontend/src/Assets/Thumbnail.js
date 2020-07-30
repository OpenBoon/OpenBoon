import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import Link from 'next/link'

import { colors, constants, spacing } from '../Styles'

import SearchSvg from '../Icons/search.svg'
import SimilaritySvg from '../Icons/similarity.svg'

import Button, { VARIANTS } from '../Button'

import { dispatch, ACTIONS } from '../Filters/helpers'
import { getQueryString } from '../Fetch/helpers'

import { formatSeconds } from './helpers'

const AssetsThumbnail = ({
  asset: {
    id,
    metadata: { source },
    thumbnailUrl,
    videoLength,
  },
  isActive,
}) => {
  const { filename } = source || {}

  const {
    pathname,
    query: { projectId, id: selectedId, query },
  } = useRouter()

  const isSelected = id === selectedId

  const queryString = getQueryString({
    ...(isSelected ? {} : { id }),
    ...(query ? { query } : {}),
  })

  const { pathname: thumbnailSrc } = new URL(thumbnailUrl)

  return (
    <div
      css={{
        position: 'relative',
        border: isSelected
          ? constants.borders.assetSelected
          : constants.borders.large.transparent,
        width: '100%',
        height: '100%',
        ':hover': isActive
          ? {
              border: isSelected
                ? constants.borders.assetSelected
                : constants.borders.large.white,
              'a, button': {
                opacity: 1,
              },
            }
          : {},
      }}
    >
      {isActive ? (
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
              borderRadius: 0,
            }}
          >
            <img
              css={{ width: '100%', height: '100%', objectFit: 'contain' }}
              src={thumbnailSrc}
              alt={filename}
            />
          </Button>
        </Link>
      ) : (
        <img
          css={{ width: '100%', height: '100%', objectFit: 'contain' }}
          src={thumbnailSrc}
          alt={filename}
        />
      )}

      {isActive && (
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
            ':hover, &.focus-visible:focus': {
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
          <SimilaritySvg
            height={constants.icons.regular}
            color={colors.structure.white}
          />
        </Button>
      )}

      {isActive && (
        <Link
          href={`/[projectId]/visualizer/[id]${getQueryString({ query })}`}
          as={`/${projectId}/visualizer/${id}${getQueryString({ query })}`}
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
              ':hover, &.focus-visible:focus': {
                opacity: 1,
                backgroundColor: colors.structure.smoke,
              },
            }}
          >
            <SearchSvg
              height={constants.icons.regular}
              color={colors.structure.white}
            />
          </Button>
        </Link>
      )}

      {videoLength > 0 && (
        <div
          css={{
            position: 'absolute',
            bottom: spacing.small,
            left: spacing.small,
            padding: spacing.mini,
            color: colors.structure.black,
            backgroundColor: `${colors.structure.white}${constants.opacity.hexHalf}`,
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
  isActive: PropTypes.bool.isRequired,
}

export default AssetsThumbnail
