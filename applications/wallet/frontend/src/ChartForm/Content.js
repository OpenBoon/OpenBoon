import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'
import { useRouter } from 'next/router'

import chartShape from '../Chart/shape'

import { colors, constants, spacing, typography } from '../Styles'

import Form from '../Form'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import { capitalizeFirstLetter } from '../Text/helpers'
import { ACTIONS } from '../DataVisualization/reducer'

import ChartFormOptions from './Options'
import { formatFields } from './helpers'

const ICON_SIZE = 20

const ChartFormContent = ({
  chart,
  chart: { type },
  chartIndex,
  dispatch,
  isEditing,
  setIsEditing,
}) => {
  const {
    query: { projectId },
  } = useRouter()

  const [attribute, setAttribute] = useState(chart.attribute || '')
  const [values, setValues] = useState(chart.values || '10')

  const { data: fields } = useSWR(
    `/api/v1/projects/${projectId}/searches/fields/`,
  )

  const filteredFields = formatFields({ fields, type })

  return (
    <Form
      style={{
        width: 'auto',
        padding: 0,
      }}
    >
      <h2
        css={{
          fontSize: typography.size.medium,
          lineHeight: typography.height.medium,
          fontWeight: typography.weight.medium,
        }}
      >
        {capitalizeFirstLetter({ word: type })} Visualization
      </h2>

      <div css={{ height: spacing.comfy }} />

      <label css={{ color: colors.structure.zinc }}>
        Metadata Type
        <select
          defaultValue={attribute}
          onChange={({ target: { value } }) => {
            setAttribute(value)
          }}
          css={{
            marginTop: spacing.base,
            width: '100%',
            padding: `${spacing.moderate}px ${spacing.base}px`,
            backgroundColor: colors.structure.steel,
            color: colors.structure.white,
            borderRadius: constants.borderRadius.small,
            border: 'none',
            fontSize: typography.size.regular,
            lineHeight: typography.height.regular,
            paddingLeft: spacing.moderate,
            MozAppearance: 'none',
            WebkitAppearance: 'none',
            backgroundImage: `url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+CiAgPHBhdGggZD0iTTE0LjI0MyA3LjU4NkwxMCAxMS44MjggNS43NTcgNy41ODYgNC4zNDMgOSAxMCAxNC42NTcgMTUuNjU3IDlsLTEuNDE0LTEuNDE0eiIgZmlsbD0iI2ZmZmZmZiIgLz4KPC9zdmc+')`,
            backgroundRepeat: `no-repeat, repeat`,
            backgroundPosition: `right ${spacing.base}px top 50%`,
            backgroundSize: ICON_SIZE,
          }}
        >
          <option value="" disabled>
            Select metadata type
          </option>
          <ChartFormOptions fields={filteredFields} type={type} path="" />
        </select>
      </label>

      {type === 'range' && <div css={{ height: spacing.spacious }} />}

      {type === 'facet' && (
        <>
          <div css={{ height: spacing.normal }} />

          <Input
            id="values"
            variant={INPUT_VARIANTS.SECONDARY}
            label="Number of Values Shown"
            type="number"
            value={values}
            onChange={({ target: { value } }) => setValues(value)}
            hasError={false}
            errorMessage=""
          />
        </>
      )}

      <div css={{ display: 'flex' }}>
        <Button
          variant={BUTTON_VARIANTS.SECONDARY}
          onClick={() => {
            if (isEditing) {
              setIsEditing(false)
              return
            }
            dispatch({ type: ACTIONS.DELETE, payload: { chartIndex } })
          }}
          css={{ flex: 1 }}
        >
          Cancel
        </Button>

        <div css={{ width: spacing.base }} />

        <Button
          variant={BUTTON_VARIANTS.PRIMARY}
          isDisabled={!attribute}
          onClick={() => {
            dispatch({
              type: ACTIONS.UPDATE,
              payload: {
                chartIndex,
                updatedChart: { ...chart, attribute, values },
              },
            })
            setIsEditing(false)
          }}
          css={{ flex: 1 }}
        >
          Save Visualization
        </Button>
      </div>
    </Form>
  )
}

ChartFormContent.propTypes = {
  chart: PropTypes.shape(chartShape).isRequired,
  chartIndex: PropTypes.number.isRequired,
  dispatch: PropTypes.func.isRequired,
  isEditing: PropTypes.bool.isRequired,
  setIsEditing: PropTypes.func.isRequired,
}

export default ChartFormContent
