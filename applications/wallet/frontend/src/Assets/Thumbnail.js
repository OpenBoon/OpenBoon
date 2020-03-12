import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import Link from 'next/link'

import assetShape from '../Asset/shape'

import { colors, constants, spacing } from '../Styles'

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

  const srcSet = files.map(
    ({ url, attrs: { width: srcWidth } }) => `${url} ${srcWidth}w`,
  )

  const { attrs: { width, height } = {} } = files[0] || {}

  const largestDimension = width > height ? 'width' : 'height'

  const pageQuery = page ? `page=${page}&` : ''

  const isSelected = id === selectedId

  return (
    <div
      css={{
        width: `${containerWidth}%`,
        height: 0,
        paddingBottom: `${containerWidth}%`,
        position: 'relative',
      }}>
      <div
        css={{
          border: isSelected ? constants.borders.assetSelected : '',
          width: '100%',
          height: '100%',
          position: 'absolute',
          padding: isSelected ? 0 : spacing.small,
          ':hover': {
            border: isSelected
              ? constants.borders.assetSelected
              : constants.borders.assetHover,
            padding: 0,
          },
        }}>
        <Link
          href={
            isSelected
              ? `/[projectId]/visualizer?${pageQuery}`
              : `/[projectId]/visualizer?${pageQuery}id=${id}`
          }
          as={
            isSelected
              ? `/${projectId}/visualizer?${pageQuery}`
              : `/${projectId}/visualizer?${pageQuery}id=${id}`
          }
          passHref>
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
            }}>
            <img
              css={{ [largestDimension]: '100%' }}
              srcSet={srcSet.join(',')}
              src={files[0] && files[0].url}
              alt={filename}
            />
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
