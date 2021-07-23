import { useState, useRef } from 'react'
import PropTypes from 'prop-types'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import { colors, constants, spacing, typography } from '../Styles'

const ModelUploadSelection = ({ dispatch }) => {
  const [isDragging, setIsDragging] = useState(false)
  const fileRef = useRef()

  return (
    <div css={{ display: 'flex' }}>
      <input
        ref={fileRef}
        type="file"
        style={{ display: 'none' }}
        onChange={(e) => {
          dispatch({ file: e.target.files[0] })
        }}
        onClick={(e) => {
          e.target.value = null
        }}
      />

      <div
        aria-label="Drag and Drop Zone"
        css={{
          color: colors.key.two,
          fontWeight: typography.weight.medium,
          fontSize: typography.size.medium,
          lineHeight: typography.height.medium,
          padding: spacing.colossal,
          paddingLeft: spacing.enormous,
          paddingRight: spacing.enormous,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          ...(isDragging
            ? {
                backgroundColor: colors.key.three,
                color: colors.key.one,
                borderStyle: 'solid',
                borderColor: colors.key.one,
                borderWidth: constants.borderWidths.medium,
              }
            : {
                borderStyle: 'dashed',
                borderColor: colors.structure.steel,
                borderWidth: constants.borderWidths.regular,
              }),
        }}
        onDragEnter={() => setIsDragging(true)}
        onDragLeave={() => setIsDragging(false)}
        onDragOver={(e) => {
          e.preventDefault()
        }}
        onDrop={(e) => {
          e.preventDefault()
          setIsDragging(false)
          dispatch({ file: e.dataTransfer.files[0] })
        }}
      >
        {isDragging ? (
          'Drop file to upload'
        ) : (
          <div>
            Drag and drop file here or{' '}
            <Button
              variant={BUTTON_VARIANTS.LINK}
              onClick={() => {
                fileRef.current?.click()
              }}
              css={{
                display: 'inline',
                textDecoration: 'underline',
                padding: 0,
                fontWeight: 'inherit',
                fontSize: 'inherit',
              }}
            >
              click to upload
            </Button>
          </div>
        )}
      </div>
    </div>
  )
}

ModelUploadSelection.propTypes = {
  dispatch: PropTypes.func.isRequired,
}

export default ModelUploadSelection
