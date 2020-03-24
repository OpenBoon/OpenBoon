import PropTypes from 'prop-types'

import { useRouter } from 'next/router'
import useSWR from 'swr'

import JsonDisplay from '../JsonDisplay'

import { colors, spacing } from '../Styles'

const ASSET_THUMBNAIL_SIZE = 48

const JobErrorAsset = ({ assetId }) => {
  const {
    query: { projectId },
  } = useRouter()

  const {
    data: asset,
    data: {
      metadata: {
        files,
        source: { filename },
      },
    },
  } = useSWR(`/api/v1/projects/${projectId}/assets/${assetId}/`)

  const srcUrl = files[0] && files[0].url

  return (
    <div
      css={{
        fontFamily: 'Roboto Mono',
        paddingBottom: spacing.spacious,
        height: 'auto',
      }}
    >
      <div
        css={{
          display: 'flex',
          alignItems: 'center',
          backgroundColor: colors.structure.iron,
          padding: spacing.normal,
        }}
      >
        {srcUrl ? (
          <img
            src={srcUrl}
            alt={filename}
            css={{
              width: ASSET_THUMBNAIL_SIZE,
              height: ASSET_THUMBNAIL_SIZE,
              objectFit: 'cover',
            }}
          />
        ) : (
          <img
            src="/icons/fallback_26.png"
            alt="Proxy Unavailable"
            css={{
              width: ASSET_THUMBNAIL_SIZE,
              height: ASSET_THUMBNAIL_SIZE,
              objectFit: 'cover',
            }}
          />
        )}
        <div css={{ paddingLeft: spacing.comfy }}>{filename}</div>
      </div>
      <div
        css={{
          padding: spacing.normal,
          backgroundColor: colors.structure.coal,
        }}
      >
        <JsonDisplay json={asset} />
      </div>
    </div>
  )
}

JobErrorAsset.propTypes = {
  assetId: PropTypes.string.isRequired,
}

export default JobErrorAsset
