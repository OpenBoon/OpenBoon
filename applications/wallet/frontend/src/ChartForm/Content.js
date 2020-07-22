import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'
import { useRouter } from 'next/router'
import chartShape from '../Chart/shape'

import { spacing, typography } from '../Styles'

import Form from '../Form'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import Listbox from '../Listbox'
import { capitalizeFirstLetter } from '../Text/helpers'
import { ACTIONS } from '../DataVisualization/reducer'

import { formatFields } from './helpers'

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

  const splitAttribute = attribute.split('.')
  const shortenedAttribute = splitAttribute[splitAttribute.length - 1]

  return (
    <Form
      style={{
        width: 'auto',
        padding: 0,
      }}
    >
      <h2
        css={{
          paddingTop: spacing.base,
          fontSize: typography.size.medium,
          lineHeight: typography.height.medium,
          fontWeight: typography.weight.medium,
        }}
      >
        {capitalizeFirstLetter({ word: type })} Visualization
      </h2>

      <div css={{ height: spacing.comfy }} />

      <Listbox
        label="Metadata Type"
        value={attribute}
        placeholder={shortenedAttribute || 'Select Type'}
        options={filteredFields}
        onChange={({ value }) => setAttribute(value)}
      />

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
