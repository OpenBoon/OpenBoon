import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'
import deepfilter from 'deep-filter'

import filterShape from '../Filter/shape'

import { spacing, constants, colors, typography } from '../Styles'

import PlusSvg from '../Icons/plus.svg'

import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import Button, { VARIANTS } from '../Button'

import { formatDisplayName } from '../Metadata/helpers'

import { dispatch, ACTIONS } from './helpers'

import FiltersMenuSection from './MenuSection'

const FiltersMenu = ({
  pathname,
  projectId,
  assetId,
  filters,
  setIsMenuOpen,
}) => {
  const { data: fields } = useSWR(
    `/api/v1/projects/${projectId}/searches/fields/`,
  )

  const [newFilters, setNewFilters] = useState({})
  const [fieldsFilter, setFieldsFilter] = useState('')

  const onClick = ({ type, attribute, modelId }) => (value) => {
    if (value) {
      const values = type === 'exists' ? { exists: true } : {}

      setNewFilters((nF) => ({
        ...nF,
        [attribute]: { type, attribute, values, modelId },
      }))
    } else {
      setNewFilters((nF) => {
        const { [attribute]: filterToRemove, ...rest } = nF
        return rest
      })
    }
  }

  const filteredFields = deepfilter(fields, (value, prop) => {
    if (
      typeof prop === 'string' &&
      Array.isArray(value) &&
      value.includes('similarity')
    ) {
      return false
    }

    if (!fieldsFilter) return true

    if (typeof prop === 'string' && Array.isArray(value)) {
      return prop.includes(fieldsFilter)
    }

    return true
  })

  return (
    <div
      css={{
        overflow: 'hidden',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
      }}
    >
      <div
        css={{
          display: 'flex',
          flexDirection: 'column',
          padding: spacing.moderate,
          borderBottom: constants.borders.regular.smoke,
        }}
      >
        <label>
          Select metadata fields to filter
          <input
            type="search"
            placeholder="Filter metadata fields"
            value={fieldsFilter}
            onChange={({ target: { value } }) => setFieldsFilter(value)}
            css={{
              marginTop: spacing.base,
              width: '100%',
              border: constants.borders.regular.transparent,
              padding: spacing.moderate,
              paddingLeft: spacing.spacious,
              borderRadius: constants.borderRadius.small,
              color: colors.structure.pebble,
              backgroundColor: colors.structure.mattGrey,
              ':hover': {
                border: constants.borders.regular.steel,
              },
              ':focus': {
                outline: constants.borders.regular.transparent,
                border: constants.borders.keyOneRegular,
                color: colors.structure.coal,
                backgroundColor: colors.structure.white,
                backgroundImage: `url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+CiAgICA8cGF0aCBmaWxsPSIjYjNiM2IzIiBkPSJNMTMuODU3IDEyLjMxNGgtLjgyM2wtLjMwOC0uMzA4YTYuNDM4IDYuNDM4IDAgMDAxLjY0NS00LjMyQTYuNjcyIDYuNjcyIDAgMDA3LjY4NiAxIDYuNjcyIDYuNjcyIDAgMDAxIDcuNjg2YTYuNjcyIDYuNjcyIDAgMDA2LjY4NiA2LjY4NSA2LjQzOCA2LjQzOCAwIDAwNC4zMi0xLjY0NWwuMzA4LjMwOHYuODIzTDE3LjQ1NyAxOSAxOSAxNy40NTdsLTUuMTQzLTUuMTQzem0tNi4xNzEgMGE0LjYxIDQuNjEgMCAwMS00LjYyOS00LjYyOCA0LjYxIDQuNjEgMCAwMTQuNjI5LTQuNjI5IDQuNjEgNC42MSAwIDAxNC42MjggNC42MjkgNC42MSA0LjYxIDAgMDEtNC42MjggNC42Mjh6Ii8+Cjwvc3ZnPg==')`,
              },

              '::placeholder': {
                fontStyle: typography.style.italic,
              },
              backgroundImage: `url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+CiAgICA8cGF0aCBmaWxsPSIjNGE0YTRhIiBkPSJNMTMuODU3IDEyLjMxNGgtLjgyM2wtLjMwOC0uMzA4YTYuNDM4IDYuNDM4IDAgMDAxLjY0NS00LjMyQTYuNjcyIDYuNjcyIDAgMDA3LjY4NiAxIDYuNjcyIDYuNjcyIDAgMDAxIDcuNjg2YTYuNjcyIDYuNjcyIDAgMDA2LjY4NiA2LjY4NSA2LjQzOCA2LjQzOCAwIDAwNC4zMi0xLjY0NWwuMzA4LjMwOHYuODIzTDE3LjQ1NyAxOSAxOSAxNy40NTdsLTUuMTQzLTUuMTQzem0tNi4xNzEgMGE0LjYxIDQuNjEgMCAwMS00LjYyOS00LjYyOCA0LjYxIDQuNjEgMCAwMTQuNjI5LTQuNjI5IDQuNjEgNC42MSAwIDAxNC42MjggNC42MjkgNC42MSA0LjYxIDAgMDEtNC42MjggNC42Mjh6Ii8+Cjwvc3ZnPg==')`,
              backgroundRepeat: `no-repeat, repeat`,
              backgroundPosition: `left ${spacing.base}px top 50%`,
              backgroundSize: constants.icons.regular,
            }}
          />
        </label>
      </div>

      <div css={{ flex: 1, overflowX: 'auto' }}>
        {Object.entries(filteredFields)
          .filter(([, value]) => Object.values(value).length > 0)
          .map(([key, value]) =>
            Array.isArray(value) ? null : (
              <Accordion
                key={key}
                variant={ACCORDION_VARIANTS.PANEL}
                title={formatDisplayName({ name: key })}
                cacheKey={`Filters.${key}`}
                isInitiallyOpen={false}
                isResizeable={false}
              >
                <div
                  css={{
                    padding: spacing.normal,
                    paddingTop: spacing.base,
                    paddingBottom: spacing.base,
                  }}
                >
                  {Object.entries(value).map(([subKey, subValue]) => {
                    return (
                      <FiltersMenuSection
                        key={subKey}
                        projectId={projectId}
                        path={key}
                        attribute={subKey}
                        value={subValue}
                        filters={filters}
                        onClick={onClick}
                      />
                    )
                  })}
                </div>
              </Accordion>
            ),
          )}
      </div>

      <div css={{ padding: spacing.base, display: 'flex' }}>
        <Button
          variant={VARIANTS.SECONDARY}
          onClick={() => setIsMenuOpen(false)}
          style={{
            flex: 1,
            display: 'flex',
            flexFlow: 'nowrap',
            alignItems: 'end',
            svg: { marginRight: spacing.base },
          }}
        >
          Cancel
        </Button>

        <div css={{ width: spacing.base, minWidth: spacing.base }} />

        <Button
          aria-label="Add Filters"
          variant={VARIANTS.PRIMARY}
          onClick={() => {
            dispatch({
              type: ACTIONS.ADD_FILTERS,
              payload: {
                pathname,
                projectId,
                assetId,
                filters,
                newFilters: Object.values(newFilters),
              },
            })

            setIsMenuOpen(false)
          }}
          style={{
            flex: 1,
            display: 'flex',
            flexFlow: 'nowrap',
            alignItems: 'end',
            svg: { marginRight: spacing.base },
          }}
          isDisabled={Object.keys(newFilters).length === 0}
        >
          <PlusSvg height={16} />
          Add Filters
        </Button>
      </div>
    </div>
  )
}

FiltersMenu.propTypes = {
  pathname: PropTypes.string.isRequired,
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  setIsMenuOpen: PropTypes.func.isRequired,
}

export default FiltersMenu
