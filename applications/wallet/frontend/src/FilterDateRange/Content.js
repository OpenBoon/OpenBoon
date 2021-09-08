import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'

import filterShape from '../Filter/shape'

import { colors, constants, spacing, typography } from '../Styles'

import CalendarSvg from '../Icons/calendar.svg'

import Slider from '../Slider'
import FilterReset from '../Filter/Reset'
import { dispatch, ACTIONS, encode } from '../Filters/helpers'
import { formatISODate } from '../Date/helpers'

import {
  formatLocaleDate,
  formatDateWithDashes,
  parseDate,
  getMinMaxFix,
} from './helpers'

const DAY_MS_VALUE = 24 * 60 * 60 * 1000

const FilterDateRangeContent = ({
  pathname,
  projectId,
  assetId,
  filters,
  filter,
  filter: {
    type,
    attribute,
    values: { min, max },
    isDisabled,
  },
  filterIndex,
}) => {
  const encodedFilter = encode({ filters: { type, attribute } })

  const { data } = useSWR(
    `/api/v1/projects/${projectId}/searches/aggregate/?filter=${encodedFilter}`,
    {
      revalidateOnFocus: false,
      revalidateOnReconnect: false,
      shouldRetryOnError: false,
    },
  )

  const { results } = data || {}

  const fallbackMin = formatISODate({ date: new Date(0) })
  const fallbackMax = formatISODate({ date: new Date() })

  const {
    minAsString: resultsMin = fallbackMin,
    maxAsString: resultsMax = fallbackMax,
  } = results || {}

  const domainMin = parseDate({ date: resultsMin })
  const domainMax = parseDate({ date: resultsMax })

  const queryMin = parseDate({ date: min })
  const queryMax = parseDate({ date: max })

  const domain = [domainMin, domainMax]

  const [dateValues, setDateValues] = useState([
    queryMin || domainMin,
    queryMax || domainMax,
  ])

  const [inputMin, setInputMin] = useState(dateValues[0])
  const [inputMax, setInputMax] = useState(dateValues[1])

  const minMaxFix = getMinMaxFix({ domainMin, domainMax })

  const sliderValues = [
    dateValues[0].getTime(),
    dateValues[1].getTime() + minMaxFix,
  ]

  const sliderDomain = [domain[0].getTime(), domain[1].getTime() + minMaxFix]

  const saveMinDate = ({ value }) => {
    if (!value) {
      setInputMin(dateValues[0])
      return
    }

    const date = parseDate({ date: value })

    if (date.toISOString() === dateValues[0].toISOString()) return

    if (date.getTime() > dateValues[1].getTime()) {
      setInputMin(dateValues[0])
      return
    }

    setInputMin(date)

    setDateValues([date, dateValues[1]])

    dispatch({
      type: ACTIONS.UPDATE_FILTER,
      payload: {
        pathname,
        projectId,
        assetId,
        filters,
        updatedFilter: {
          type,
          attribute,
          values: {
            min: date.toISOString().replace(/T\d\d:\d\d:\d\d/, 'T00:00:00'),
            max: dateValues[1]
              .toISOString()
              .replace(/T\d\d:\d\d:\d\d/, 'T23:59:59'),
          },
        },
        filterIndex,
      },
    })
  }

  const saveMaxDate = ({ value }) => {
    if (!value) {
      setInputMax(dateValues[1])
      return
    }

    const date = parseDate({ date: value })

    if (date.toISOString() === dateValues[1].toISOString()) return

    if (date.getTime() < dateValues[0].getTime()) {
      setInputMax(dateValues[1])
      return
    }

    setInputMax(date)

    setDateValues([dateValues[0], date])

    dispatch({
      type: ACTIONS.UPDATE_FILTER,
      payload: {
        pathname,
        projectId,
        assetId,
        filters,
        updatedFilter: {
          type,
          attribute,
          values: {
            min: dateValues[0]
              .toISOString()
              .replace(/T\d\d:\d\d:\d\d/, 'T00:00:00'),
            max: date.toISOString().replace(/T\d\d:\d\d:\d\d/, 'T23:59:59'),
          },
        },
        filterIndex,
      },
    })
  }

  return (
    <>
      <FilterReset
        pathname={pathname}
        projectId={projectId}
        assetId={assetId}
        filters={filters}
        filter={filter}
        filterIndex={filterIndex}
        onReset={() => {
          setDateValues(domain)
          setInputMin(domain[0])
          setInputMax(domain[1])
        }}
      />
      <div css={{ padding: spacing.normal }}>
        <div
          css={{
            display: 'flex',
            justifyContent: 'space-between',
            paddingBottom: spacing.normal,
            fontFamily: typography.family.mono,
          }}
        >
          <span>{formatLocaleDate({ date: domainMin })}</span>
          <span>{formatLocaleDate({ date: domainMax })}</span>
        </div>
        <div css={{ padding: spacing.small }}>
          <Slider
            mode="both"
            step={DAY_MS_VALUE}
            domain={sliderDomain}
            values={sliderValues}
            isMuted={!!isDisabled}
            isDisabled={false}
            onUpdate={(values) => {
              const formattedValues = [new Date(values[0]), new Date(values[1])]
              setDateValues(formattedValues)
              setInputMin(formattedValues[0])
              setInputMax(formattedValues[1])
            }}
            onChange={([newMin, newMax]) => {
              dispatch({
                type: ACTIONS.UPDATE_FILTER,
                payload: {
                  pathname,
                  projectId,
                  assetId,
                  filters,
                  updatedFilter: {
                    type,
                    attribute,
                    values: {
                      min: new Date(newMin)
                        .toISOString()
                        .replace(/T\d\d:\d\d:\d\d/, 'T00:00:00'),
                      max: new Date(newMax)
                        .toISOString()
                        .replace(/T\d\d:\d\d:\d\d/, 'T23:59:59'),
                    },
                  },
                  filterIndex,
                },
              })
            }}
          />
        </div>
        <div
          css={{
            paddingTop: spacing.large,
            color: colors.structure.zinc,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}
        >
          <label
            css={{
              display: 'flex',
              alignItems: 'center',
              position: 'relative',
            }}
          >
            <CalendarSvg
              css={{
                position: 'absolute',
                right: 6,
                top: 10,
                pointerEvents: 'none',
              }}
              color={colors.structure.steel}
              height={constants.icons.small}
            />
            <input
              type="date"
              min={formatDateWithDashes({ date: domain[0] })}
              max={formatDateWithDashes({ date: inputMax })}
              css={{
                textAlign: 'center',
                paddingLeft: spacing.mini,
                paddingRight: spacing.mini,
                paddingTop: spacing.base,
                paddingBottom: spacing.base,
                border: constants.borders.regular.transparent,
                borderRadius: constants.borderRadius.small,
                backgroundColor: colors.structure.lead,
                color: colors.structure.white,
                ':hover': {
                  border: constants.borders.regular.steel,
                },
                ':focus': {
                  outline: constants.borders.regular.transparent,
                  border: constants.borders.keyOneRegular,
                  color: colors.structure.coal,
                  backgroundColor: colors.structure.white,
                },
                '::placeholder': {
                  fontStyle: typography.style.italic,
                },
                '::-webkit-datetime-edit-month-field, ::-webkit-datetime-edit-day-field, ::-webkit-datetime-edit-year-field':
                  { color: colors.structure.white },
              }}
              value={inputMin ? formatDateWithDashes({ date: inputMin }) : ''}
              onChange={({ target: { value } }) => {
                setInputMin(parseDate({ date: value }))
              }}
              onBlur={({ target: { value } }) => {
                saveMinDate({ value })
              }}
            />
          </label>
          <div
            css={{
              width: 16,
              borderBottom: '2px solid white',
            }}
          />
          <label
            css={{
              display: 'flex',
              alignItems: 'center',
              position: 'relative',
            }}
          >
            <CalendarSvg
              css={{
                position: 'absolute',
                right: 6,
                top: 10,
                pointerEvents: 'none',
              }}
              color={colors.structure.steel}
              height={constants.icons.small}
            />
            <input
              type="date"
              min={formatDateWithDashes({ date: inputMin })}
              max={formatDateWithDashes({ date: domain[1] })}
              css={{
                textAlign: 'center',
                paddingLeft: spacing.mini,
                paddingRight: spacing.mini,
                paddingTop: spacing.base,
                paddingBottom: spacing.base,
                border: constants.borders.regular.transparent,
                borderRadius: constants.borderRadius.small,
                backgroundColor: colors.structure.lead,
                color: colors.structure.white,
                ':hover': {
                  border: constants.borders.regular.steel,
                },
                ':focus': {
                  outline: constants.borders.regular.transparent,
                  border: constants.borders.keyOneRegular,
                  color: colors.structure.coal,
                  backgroundColor: colors.structure.white,
                },
                '::placeholder': {
                  fontStyle: typography.style.italic,
                },
                '::-webkit-datetime-edit-month-field, ::-webkit-datetime-edit-day-field, ::-webkit-datetime-edit-year-field':
                  { color: colors.structure.white },
              }}
              value={inputMax ? formatDateWithDashes({ date: inputMax }) : ''}
              onChange={({ target: { value } }) => {
                setInputMax(parseDate({ date: value }))
              }}
              onBlur={({ target: { value } }) => {
                saveMaxDate({ value })
              }}
            />
          </label>
        </div>
      </div>
    </>
  )
}

FilterDateRangeContent.propTypes = {
  pathname: PropTypes.string.isRequired,
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterDateRangeContent
