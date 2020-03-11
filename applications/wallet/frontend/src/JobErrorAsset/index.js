import PropTypes from 'prop-types'

import { useRouter } from 'next/router'
import useSWR from 'swr'
import JSONPretty from 'react-json-pretty'

import { spacing, colors } from '../Styles'

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
        height: '100%',
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
            width: '48px',
            height: '48px',
            padding: spacing.small,
            border: '1px solid grey',
          }}
        />
        <div css={{ paddingLeft: spacing.comfy }}>{filename}</div>
      </div>
      <div css={{ padding: spacing.normal }}>
        <JSONPretty
          id="json-pretty"
          data={asset}
          theme={{
            main: 'line-height:1.3;overflow:auto;',
            string: `color:${colors.signal.grass.base};`,
            value: `color:${colors.signal.sky.base};`,
            boolean: `color:${colors.signal.canary.base};`,
          }}
        />
      </div>
    </div>
  )
}

JobErrorAsset.propTypes = {
  assetId: PropTypes.string.isRequired,
}

export default JobErrorAsset
