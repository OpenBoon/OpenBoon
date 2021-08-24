import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'
import deepfilter from 'deep-filter'

import filterShape from '../Filter/shape'

import { spacing, constants } from '../Styles'

import PlusSvg from '../Icons/plus.svg'

import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import Button, { VARIANTS } from '../Button'

import { formatDisplayName } from '../Metadata/helpers'

import { getValues, dispatch, ACTIONS } from './helpers'

import FiltersMenuSection from './MenuSection'

import InputSearch, { VARIANTS as INPUT_SEARCH_VARIANTS } from '../Input/Search'

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

  const onClick = ({ type, attribute, datasetId }) => {
    return (value) => {
      if (value) {
        const values = getValues({ type })

        setNewFilters((nF) => ({
          ...nF,
          [attribute]: { type, attribute, values, datasetId },
        }))
      } else {
        setNewFilters((nF) => {
          const { [attribute]: filterToRemove, ...rest } = nF
          return rest
        })
      }
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
          flexShrink: 0,
        }}
      >
        <label>
          Select metadata fields to filter
          <InputSearch
            placeholder="Filter metadata fields"
            value={fieldsFilter}
            onChange={({ value }) => setFieldsFilter(value)}
            variant={INPUT_SEARCH_VARIANTS.DARK}
            style={{ marginTop: spacing.base }}
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

      <div css={{ padding: spacing.base, display: 'flex', flexShrink: 0 }}>
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
          type="submit"
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
