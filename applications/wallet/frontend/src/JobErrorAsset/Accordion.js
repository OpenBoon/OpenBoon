import PropTypes from 'prop-types'
import JSONPretty from 'react-json-pretty'
import { useState } from 'react'
import assetShape from '../Asset/shape'
import { colors, spacing, constants } from '../Styles'

import ChevronSvg from '../Icons/chevron.svg'

const ASSET_THUMBNAIL_SIZE = 80
const CHEVRON_WIDTH = 20

const JobErrorAssetAccordion = ({
  asset,
  asset: {
    metadata: {
      source: { filename, url },
    },
  },
  isCollapsible,
}) => {
  const [isErrorAssetOpen, setErrorAssetOpen] = useState(false)
  return (
    <div>
      <div
        css={{
          backgroundColor: colors.structure.lead,
          boxShadow: constants.boxShadows.default,
          borderBottom: constants.borders.tabs,
          'div:last-child > &': {
            borderBottom: 'none',
          },
          padding: spacing.normal,
          display: 'flex',
          justifyContent: 'space-between',
          ':hover': {
            backgroundColor: isCollapsible && colors.structure.iron,
            '.chevron': {
              color: colors.structure.white,
            },
          },
          cursor: isCollapsible && 'pointer',
        }}
        role="button"
        tabIndex="0"
        onClick={() => {
          setErrorAssetOpen(!isErrorAssetOpen)
        }}
        onKeyDown={() => {
          setErrorAssetOpen(!isErrorAssetOpen)
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

        {isCollapsible && (
          <div
            className="chevron"
            css={{ display: 'flex', color: colors.structure.iron }}>
            <ChevronSvg
              width={CHEVRON_WIDTH}
              css={{
                marginLeft: spacing.base,
                transform: `${isErrorAssetOpen ? 'rotate(-180deg)' : ''}`,
              }}
            />
          </div>
        )}
      </div>
      <div
        css={{
          paddingBottom: spacing.spacious,
          display: isCollapsible && !isErrorAssetOpen && 'none',
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
      </div>
    </div>
  )
}

JobErrorAssetAccordion.propTypes = {
  asset: assetShape.isRequired,
  isCollapsible: PropTypes.bool.isRequired,
}

export default JobErrorAssetAccordion
