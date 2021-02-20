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
    id: thumbnailId,
    metadata: { source },
    thumbnailUrl,
    videoLength,
  },
  isActive,
  attribute,
}) => {
  const { filename } = source || {}

  const {
    query: { projectId, assetId, query },
  } = useRouter()

  const isSelected = thumbnailId === assetId

  const queryString = getQueryString({
    ...(isSelected ? {} : { assetId: thumbnailId }),
    ...(query ? { query } : {}),
  })

  const { pathname: thumbnailSrc } = new URL(thumbnailUrl)

  return (
    <div
      css={{
        position: 'relative',
        border: isSelected
          ? constants.borders.keyOneLarge
          : constants.borders.large.transparent,
        width: '100%',
        height: '100%',
        ':hover': isActive
          ? {
              border: isSelected
                ? constants.borders.keyOneLarge
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
            title={filename}
            aria-label={`Select asset ${filename}`}
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
          title="Find similar images"
          aria-label={`Find similar images to ${filename}`}
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
                projectId,
                thumbnailId,
                assetId,
                query,
                attribute,
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
          href={`/[projectId]/visualizer/[assetId]${getQueryString({ query })}`}
          as={`/${projectId}/visualizer/${thumbnailId}${getQueryString({
            query,
          })}`}
          passHref
        >
          <Button
            title="Asset details"
            aria-label={`Asset details ${filename}`}
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

AssetsThumbnail.defaultProps = {
  attribute: 'analysis.boonai-image-similarity',
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
  attribute: PropTypes.string,
}

export default AssetsThumbnail
