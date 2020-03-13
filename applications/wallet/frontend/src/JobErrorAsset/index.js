import PropTypes from 'prop-types'
import JSONPretty from 'react-json-pretty'

import { useRouter } from 'next/router'
import useSWR from 'swr'

<<<<<<< HEAD
import JsonDisplay from '../JsonDisplay'

import { colors, constants, spacing } from '../Styles'

const ASSET_THUMBNAIL_SIZE = 48
=======
import { colors, spacing, constants } from '../Styles'

const ASSET_THUMBNAIL_SIZE = 80
>>>>>>> consolidate asset code

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
<<<<<<< HEAD
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
            padding: spacing.small,
            border: constants.borders.separator,
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
=======
    <div>
      <div
        css={{
          backgroundColor: colors.structure.lead,
          boxShadow: constants.boxShadows.default,
          padding: spacing.normal,
          display: 'flex',
          justifyContent: 'space-between',
        }}>
        <div
          css={{
            display: 'flex',
            alignItems: 'center',
            fontFamily: 'Roboto Mono',
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
      </div>
      <div
        css={{
          paddingBottom: spacing.spacious,
          height: 'auto',
        }}>
        <div css={{ backgroundColor: colors.structure.black }}>
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
>>>>>>> consolidate asset code
      </div>
    </div>
  )
}

JobErrorAsset.propTypes = {
  assetId: PropTypes.string.isRequired,
}

export default JobErrorAsset
