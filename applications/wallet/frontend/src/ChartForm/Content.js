import { useReducer } from 'react'
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

const INITIAL_STATE = ({ attribute, values }) => ({
  attribute: attribute || '',
  values: values || '10',
})

const reducer = (state, action) => ({ ...state, ...action })

const ChartFormContent = ({
  chart,
  chart: { type, attribute, values },
  chartIndex,
  dispatch,
  isEditing,
  setIsEditing,
}) => {
  const {
    query: { projectId },
  } = useRouter()

  const initializedState = INITIAL_STATE({ attribute, values })

  const [state, formDispatch] = useReducer(reducer, initializedState)

  const { data: fields } = useSWR(
    `/api/v1/projects/${projectId}/searches/fields/`,
  )

  const filteredFields = formatFields({ fields, type })

  const splitAttribute = state.attribute.split('.')
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
        inputLabel="Filter types"
        value={state.attribute}
        placeholder={shortenedAttribute || 'Select Type'}
        options={filteredFields}
        onChange={({ value }) => {
          formDispatch({ attribute: value })
        }}
      />

      {(type === 'facet' || type === 'histogram') && (
        <>
          <div css={{ height: spacing.normal }} />

          <Input
            id="values"
            variant={INPUT_VARIANTS.SECONDARY}
            label="Number of Values Shown"
            type="number"
            value={state.values}
            onChange={({ target: { value } }) =>
              formDispatch({ values: value })
            }
            hasError={false}
            errorMessage=""
          />
        </>
      )}

      {type === 'range' && <div css={{ height: spacing.spacious }} />}

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
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          isDisabled={!state.attribute}
          onClick={() => {
            dispatch({
              type: ACTIONS.UPDATE,
              payload: {
                chartIndex,
                updatedChart: {
                  ...chart,
                  ...state,
                },
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
