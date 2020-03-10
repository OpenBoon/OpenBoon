import PropTypes from 'prop-types'
import JSONPretty from 'react-json-pretty'

import { spacing, colors } from '../Styles'

const JobErrorAssetContent = ({ asset }) => {
  return (
    <>
      <div
        css={{
          backgroundColor: colors.structure.black,
          fontFamily: 'Roboto Mono',
          padding: spacing.normal,
          overflow: 'auto',
        }}>
        <img
          src={asset.metadata.source.url.replace(
            'https://wallet.zmlp.zorroa.com',
            '',
          )}
          alt=""
        />
        {asset.metadata.source.filename}

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
    </>
  )
}

JobErrorAssetContent.propTypes = {
  asset: PropTypes.shape({
    metadata: PropTypes.shape({
      source: PropTypes.shape({
        url: PropTypes.string.isRequired,
        filename: PropTypes.string.isRequired,
      }).isRequired,
    }).isRequired,
  }).isRequired,
}

export default JobErrorAssetContent
