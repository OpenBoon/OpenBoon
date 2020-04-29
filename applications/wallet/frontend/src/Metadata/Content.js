import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, constants, spacing } from '../Styles'

import useLocalStorage from '../LocalStorage'

import Button, { VARIANTS } from '../Button'
import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import JsonDisplay from '../JsonDisplay'

import MetadataPretty from './Pretty'
import { formatDisplayName } from './helpers'

const DISPLAY_OPTIONS = ['pretty', 'raw json']

const MetadataContent = ({ projectId, assetId }) => {
  const [displayOption, setDisplayOption] = useLocalStorage({
    key: 'metadataFormat',
    initialValue: 'pretty',
  })

  const { data: asset } = useSWR(
    `/api/v1/projects/${projectId}/assets/${assetId}/`,
  )

  const {
    metadata,
    metadata: {
      source: { filename },
    },
  } = asset

  return (
    <>
      <div
        css={{
          padding: spacing.normal,
          borderBottom: constants.borders.divider,
        }}
      >
        <div css={{ color: colors.signal.sky.base }}>{filename}</div>
      </div>

      <div
        css={{ padding: spacing.base, borderBottom: constants.borders.divider }}
      >
        <div
          css={{
            display: 'flex',
            width: 'fit-content',
            border: constants.borders.tableRow,
            borderRadius: constants.borderRadius.small,
          }}
        >
          {DISPLAY_OPTIONS.map((value) => (
            <Button
              key={value}
              style={{
                borderRadius: 0,
                border: constants.borders.transparent,
                paddingTop: spacing.base,
                paddingBottom: spacing.base,
                paddingLeft: spacing.moderate,
                paddingRight: spacing.moderate,
                backgroundColor:
                  displayOption === value
                    ? colors.structure.steel
                    : colors.transparent,
                color:
                  displayOption === value
                    ? colors.structure.white
                    : colors.structure.steel,
                ':hover': {
                  color: colors.structure.white,
                },
                textTransform: 'uppercase',
              }}
              variant={VARIANTS.NEUTRAL}
              onClick={() => setDisplayOption({ value })}
            >
              {value}
            </Button>
          ))}
        </div>
      </div>

      {displayOption === 'pretty' && (
        <div css={{ overflow: 'auto' }}>
          {Object.keys(metadata).map((section) => {
            const title = formatDisplayName({ name: section })

            return (
              <Accordion
                key={section}
                variant={ACCORDION_VARIANTS.PANEL}
                title={title}
                isInitiallyOpen
              >
                <MetadataPretty metadata={metadata} section={section} />
              </Accordion>
            )
          })}
        </div>
      )}

      {displayOption === 'raw json' && (
        <div
          css={{
            height: '100%',
            overflow: 'auto',
            backgroundColor: colors.structure.coal,
            pre: {
              padding: spacing.normal,
            },
          }}
        >
          <JsonDisplay json={asset} />
        </div>
      )}
    </>
  )
}

MetadataContent.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
}

export default MetadataContent
