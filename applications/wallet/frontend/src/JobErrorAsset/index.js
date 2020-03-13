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
        source: { filename, url },
      },
    },
  } = useSWR(`/api/v1/projects/${projectId}/assets/${assetId}/`)

  return (
    <div
      css={{
        fontFamily: 'Roboto Mono',
        paddingBottom: spacing.spacious,
        height: 'auto',
      }}>
      <div
        css={{
          display: 'flex',
          alignItems: 'center',
          backgroundColor: colors.structure.lead,
          padding: spacing.normal,
        }}>
        <img
          src={url.replace('https://wallet.zmlp.zorroa.com', '')}
          alt={filename}
          css={{
            width: ASSET_THUMBNAIL_SIZE,
            height: ASSET_THUMBNAIL_SIZE,
            objectFit: 'cover',
          }}
        />
        <div css={{ paddingLeft: spacing.comfy }}>{filename}</div>
      </div>
      <div
        css={{
          padding: spacing.normal,
          backgroundColor: colors.structure.black,
        }}>
        <JsonDisplay json={asset} />
      </div>
    </div>
  )
}

JobErrorAsset.propTypes = {
  assetId: PropTypes.string.isRequired,
}

export default JobErrorAsset
