import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import Link from 'next/link'

import assetShape from '../Asset/shape'

import { colors, constants } from '../Styles'

import Button, { VARIANTS } from '../Button'

const AssetsThumbnail = ({
  asset: {
    id,
    metadata: {
      files,
      source: { filename },
    },
  },
  thumbnailCount,
}) => {
  const {
    query: { projectId, page, id: selectedId },
  } = useRouter()

  const containerWidth = 100 / thumbnailCount
  const srcUrl = files[0] && files[0].url

  const srcSet = files.map(
    ({ url, attrs: { width: srcWidth } }) => `${url} ${srcWidth}w`,
  )

  const { attrs: { width, height } = {} } = files[0] || {}

  const largestDimension = width > height ? 'width' : 'height'

  const isSelected = id === selectedId
  const queryString = [page ? `page=${page}` : '', isSelected ? '' : `id=${id}`]
    .filter(Boolean)
    .join('&')
  const queryParams = queryString ? `?${queryString}` : ''

  return (
    <div
      css={{
        width: `${containerWidth}%`,
        height: 0,
        paddingBottom: `${containerWidth}%`,
        position: 'relative',
        minWidth: 100,
        minHeight: 100,
      }}
    >
      <div
        css={{
          border: isSelected
            ? constants.borders.assetSelected
            : constants.borders.assetInactive,
          width: '100%',
          height: '100%',
          position: 'absolute',
          ':hover': {
            border: isSelected
              ? constants.borders.assetSelected
              : constants.borders.assetHover,
          },
        }}
      >
        <Link
          href={`/[projectId]/visualizer${queryParams}`}
          as={`/${projectId}/visualizer${queryParams}`}
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
            {srcUrl ? (
              <img
                css={{ [largestDimension]: '100%' }}
                srcSet={srcSet.join(',')}
                src={srcUrl}
                alt={filename}
              />
            ) : (
              <img
                srcSet="/icons/fallback.png 256w, /icons/fallback_2x.png 512w, /icons/fallback_3x.png 1024w"
                alt="Proxy Unavailable"
                src="/icons/fallback.png"
                css={{ width: '100%' }}
              />
            )}
          </Button>
        </Link>
      </div>
    </div>
  )
}

AssetsThumbnail.propTypes = {
  asset: assetShape.isRequired,
  thumbnailCount: PropTypes.number.isRequired,
}

export default AssetsThumbnail
