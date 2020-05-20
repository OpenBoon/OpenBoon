import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import Link from 'next/link'

import { colors, constants, spacing } from '../Styles'

import ExpandSvg from '../Icons/expand.svg'

import Button, { VARIANTS } from '../Button'

const AssetsThumbnail = ({
  asset: {
    id,
    metadata: {
      source: { filename },
    },
    thumbnailUrl,
    assetStyle,
    videoLength,
  },
}) => {
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

  const { pathname: src } = new URL(thumbnailUrl)

  return (
    <div
      css={{
        display: 'relative',
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
            svg: {
              display: 'inline-block',
            },
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
          <img
            css={{ width: '100%', height: '100%', objectFit: 'contain' }}
            src={src}
            alt={filename}
          />
        </Button>
      </Link>

      {assetStyle === 'video' && (
        <>
          <div
            css={{
              position: 'absolute',
              bottom: spacing.base,
              left: spacing.base,
              padding: spacing.mini,
              color: colors.structure.black,
              // Append 80 for half opacity without affecting text
              backgroundColor: `${colors.structure.white}80`,
            }}
          >
            {videoLength}
          </div>
          <Link
            href={`/[projectId]/visualizer/[id]${queryString}`}
            as={`/${projectId}/visualizer/${id}${queryString}`}
            passHref
          >
            <Button
              variant={VARIANTS.NEUTRAL}
              style={{
                position: 'absolute',
                bottom: spacing.base,
                right: spacing.base,
                padding: spacing.small,
                backgroundColor: colors.structure.smoke,
                opacity: constants.opacity.half,
                ':hover': {
                  opacity: constants.opacity.eighth,
                },
              }}
            >
              <ExpandSvg
                width={20}
                color={colors.structure.white}
                css={{
                  display: 'none',
                }}
              />
            </Button>
          </Link>
        </>
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
  }).isRequired,
}

export default AssetsThumbnail
