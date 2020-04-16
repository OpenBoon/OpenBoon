import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, constants, spacing } from '../Styles'

import useLocalStorage from '../LocalStorage'

import Button, { VARIANTS } from '../Button'
import JsonDisplay from '../JsonDisplay'

import MetadataPretty from './Pretty'

const DISPLAY_OPTIONS = [
  { value: 'pretty', name: 'PRETTY' },
  {
    value: 'rawJson',
    name: 'RAW JSON',
  },
]

const MetadataContent = ({ projectId, assetId }) => {
  const [displayOption, setDisplayOption] = useLocalStorage({
    key: 'displayOption',
    initialValue: 'pretty',
  })

  const { data: asset } = useSWR(
    `/api/v1/projects/${projectId}/assets/${assetId}/`,
  )

  const {
    metadata: {
      source: { filename },
    },
  } = asset

  return (
    <div css={{ height: '100%', backgroundColor: colors.structure.lead }}>
      <div
        css={{
          padding: spacing.normal,
          borderBottom: constants.borders.divider,
        }}
      >
        <div
          css={{
            color: colors.signal.sky.base,
          }}
        >
          {filename}
        </div>
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
          {DISPLAY_OPTIONS.map(({ value, name }) => (
            <Button
              key={value}
              style={{
                borderRadius: 0,
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
              }}
              variant={VARIANTS.NEUTRAL}
              onClick={() => setDisplayOption({ value })}
            >
              {name}
            </Button>
          ))}
        </div>
      </div>

      {displayOption === 'pretty' && <MetadataPretty asset={asset} />}

      {displayOption === 'rawJson' && (
        <div
          css={{
            height: '100%',
            overflow: 'auto',
            backgroundColor: colors.structure.coal,
            padding: spacing.normal,
          }}
        >
          <JsonDisplay json={asset} />
        </div>
      )}
    </div>
  )
}

MetadataContent.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
}

export default MetadataContent
