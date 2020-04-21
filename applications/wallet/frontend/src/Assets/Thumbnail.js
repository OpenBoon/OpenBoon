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
}) => {
  const {
    query: { projectId, id: selectedId },
  } = useRouter()

  const { url: srcUrl, attrs: { width, height } = {} } =
    files.find(({ mimetype }) => mimetype === 'image/jpeg') || {}

  const srcSet = files
    .filter(({ mimetype }) => mimetype === 'image/jpeg')
    .map(({ url, attrs: { width: srcWidth } }) => `${url} ${srcWidth}w`)

  const largestDimension = width > height ? 'width' : 'height'

  const isSelected = id === selectedId
  const queryString = isSelected ? '' : `id=${id}`
  const queryParams = queryString ? `?${queryString}` : ''

  return (
    <div
      css={{
        border: isSelected
          ? constants.borders.assetSelected
          : constants.borders.assetInactive,
        width: '100%',
        height: '100%',
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
              srcSet={srcSet.join(', ')}
              src={srcUrl}
              alt={filename}
            />
          ) : (
            <img
              srcSet="/icons/fallback.png 256w, /icons/fallback_2x.png 512w, /icons/fallback_3x.png 1024w"
              alt={filename}
              src="/icons/fallback.png"
              css={{ width: '100%' }}
            />
          )}
        </Button>
      </Link>
    </div>
  )
}

AssetsThumbnail.propTypes = {
  asset: assetShape.isRequired,
}

export default AssetsThumbnail
