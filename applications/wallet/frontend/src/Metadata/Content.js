import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, constants, spacing, typography } from '../Styles'

import { useLocalStorage } from '../LocalStorage/helpers'

import ButtonCopy from '../Button/Copy'
import Button, { VARIANTS } from '../Button'
import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import JsonDisplay from '../JsonDisplay'
import MetadataPretty from '../MetadataPretty'
import InputSearch, { VARIANTS as INPUT_SEARCH_VARIANTS } from '../Input/Search'

import { filter, formatDisplayName } from './helpers'

const DISPLAY_OPTIONS = ['pretty', 'raw json']

const MetadataContent = ({ projectId, assetId }) => {
  const [displayOption, setDisplayOption] = useLocalStorage({
    key: 'metadataFormat',
    initialState: 'pretty',
  })

  const {
    data: {
      metadata,
      metadata: {
        source: { filename },
      },
    },
  } = useSWR(`/api/v1/projects/${projectId}/assets/${assetId}/`)

  const [searchString, setSearchString] = useLocalStorage({
    key: 'MetadataContent.filter',
    initialState: '',
  })

  const filteredMetadata = filter({ metadata, searchString })

  return (
    <>
      <div
        css={{
          padding: spacing.base,
          paddingLeft: spacing.normal,
          borderBottom: constants.borders.regular.smoke,
          color: colors.structure.pebble,
        }}
      >
        {filename}
      </div>

      <div
        css={{
          display: 'flex',
          // compensates for unexcepted height compression in Safari
          minHeight: 'fit-content',
          borderBottom: constants.borders.regular.smoke,
          color: colors.structure.steel,
          svg: { opacity: 0 },
          ':hover': {
            backgroundColor: `${colors.signal.sky.base}${constants.opacity.hex22Pct}`,
            color: colors.structure.white,
            div: {
              svg: { opacity: 1 },
            },
          },
        }}
      >
        <div
          css={{
            fontFamily: typography.family.condensed,
            padding: spacing.base,
            paddingLeft: spacing.normal,
          }}
        >
          ID
        </div>

        <div
          title={assetId}
          css={{
            flex: 1,
            fontFamily: typography.family.mono,
            fontSize: typography.size.small,
            lineHeight: typography.height.small,
            color: colors.structure.pebble,
            padding: spacing.base,
            wordBreak: 'break-all',
          }}
        >
          {assetId}
        </div>

        <div
          css={{
            display: 'flex',
            alignItems: 'center',
            paddingRight: spacing.base,
          }}
        >
          <ButtonCopy title="Asset ID" value={assetId} offset={100} />
        </div>
      </div>

      <div
        css={{
          padding: spacing.base,
          borderBottom: constants.borders.regular.smoke,
        }}
      >
        <div
          css={{
            display: 'flex',
            width: 'fit-content',
            border: constants.borders.regular.steel,
            borderRadius: constants.borderRadius.small,
          }}
        >
          {DISPLAY_OPTIONS.map((value) => (
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
                    : colors.structure.transparent,
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
        <div
          css={{
            padding: spacing.base,
            borderBottom: constants.borders.regular.smoke,
          }}
        >
          <InputSearch
            aria-label="Filter metadata fields"
            placeholder="Filter metadata fields"
            value={searchString}
            onChange={({ value }) => setSearchString({ value })}
            variant={INPUT_SEARCH_VARIANTS.DARK}
          />
        </div>
      )}

      {displayOption === 'pretty' && (
        <div css={{ overflow: 'auto' }}>
          {Object.keys(filteredMetadata)
            .sort()
            .map((section) => {
              const title = formatDisplayName({ name: section })

              return (
                <Accordion
                  key={section}
                  variant={ACCORDION_VARIANTS.PANEL}
                  title={title}
                  cacheKey={`Metadata.${section}`}
                  isInitiallyOpen={false}
                  isResizeable={false}
                >
                  <MetadataPretty
                    metadata={filteredMetadata}
                    section={section}
                  />
                </Accordion>
              )
            })}
        </div>
      )}

      {displayOption === 'raw json' && (
        <div css={{ height: '100%', overflow: 'auto' }}>
          <JsonDisplay json={metadata} />
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
