import { useState } from 'react'
import PropTypes from 'prop-types'

import filterShape from '../Filter/shape'

import { spacing } from '../Styles'

import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import Button, { VARIANTS } from '../Button'

import { formatDisplayName } from '../Metadata/helpers'

import { dispatch, ACTIONS } from './helpers'

import FiltersMenuSection from './MenuSection'

const FiltersMenu = ({
  projectId,
  assetId,
  filters,
  fields,
  setIsMenuOpen,
}) => {
  const [newFilters, setNewFilters] = useState({})

  const onClick = ({ type, attribute }) => (value) => {
    if (value) {
      const values = type === 'exists' ? { exists: true } : {}

      setNewFilters((nF) => ({
        ...nF,
        [attribute]: { type, attribute, values },
      }))
    } else {
      setNewFilters((nF) => {
        const { [attribute]: filterToRemove, ...rest } = nF
        return rest
      })
    }
  }

  return (
    <div
      css={{
        overflow: 'hidden',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        flex: 1,
      }}
    >
      <div css={{ overflowX: 'auto' }}>
        {Object.entries(fields).map(([key, value]) =>
          Array.isArray(value) ? null : (
            <Accordion
              key={key}
              variant={ACCORDION_VARIANTS.PANEL}
              title={formatDisplayName({ name: key })}
              cacheKey={`FiltersMenu.${key}`}
              isInitiallyOpen={false}
            >
              <div
                css={{
                  padding: spacing.normal,
                  paddingTop: spacing.base,
                  paddingBottom: spacing.base,
                }}
              >
                {Object.entries(value).map(([subKey, subValue]) => (
                  <FiltersMenuSection
                    key={subKey}
                    path={key}
                    attribute={subKey}
                    value={subValue}
                    filters={filters}
                    onClick={onClick}
                  />
                ))}
              </div>
            </Accordion>
          ),
        )}
      </div>

      <div css={{ padding: spacing.base, display: 'flex' }}>
        <Button
          variant={VARIANTS.SECONDARY}
          onClick={() => setIsMenuOpen(false)}
          style={{ flex: 1 }}
        >
          x Cancel
        </Button>

        <div css={{ width: spacing.base }} />

        <Button
          variant={VARIANTS.PRIMARY}
          onClick={() => {
            dispatch({
              action: ACTIONS.ADD_FILTERS,
              payload: {
                projectId,
                assetId,
                filters,
                newFilters: Object.values(newFilters),
              },
            })

            setIsMenuOpen(false)
          }}
          style={{ flex: 1 }}
          isDisabled={Object.keys(newFilters).length === 0}
        >
          + Add Selected Filters
        </Button>
      </div>
    </div>
  )
}

FiltersMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  fields: PropTypes.objectOf(PropTypes.objectOf).isRequired,
  setIsMenuOpen: PropTypes.func.isRequired,
}

export default FiltersMenu
