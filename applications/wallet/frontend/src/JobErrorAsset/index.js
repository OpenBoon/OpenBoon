import PropTypes from 'prop-types'

import { useRouter } from 'next/router'
import useSWR from 'swr'

import JsonDisplay from '../JsonDisplay'

import { spacing, colors, constants } from '../Styles'

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
        backgroundColor: colors.structure.black,
        fontFamily: 'Roboto Mono',
        height: 'auto',
      }}>
      <div
        css={{
          display: 'flex',
          alignItems: 'center',
          background: colors.structure.mattGrey,
          padding: spacing.normal,
        }}>
        <img
          src={url.replace('https://wallet.zmlp.zorroa.com', '')}
          alt={filename}
          css={{
            width: ASSET_THUMBNAIL_SIZE,
            height: ASSET_THUMBNAIL_SIZE,
            padding: spacing.small,
            border: constants.borders.separator,
          }}
        />
        <div css={{ paddingLeft: spacing.comfy }}>{filename}</div>
      </div>
      <div css={{ padding: spacing.normal }}>
        <JsonDisplay json={asset} />
      </div>
    </div>
  )
}

JobErrorAsset.propTypes = {
  assetId: PropTypes.string.isRequired,
}

export default JobErrorAsset
