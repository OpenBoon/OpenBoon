/* eslint-disable jsx-a11y/label-has-associated-control */
import { useState } from 'react'
import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

import ImagesSvg from '../Icons/images.svg'
import DocumentsSvg from '../Icons/documents.svg'
import VideosSvg from '../Icons/videos.svg'

import checkboxOptionShape from './optionShape'

import CheckboxIcon from './Icon'

const CheckboxTableRow = ({
  option: { value, label, initialValue, isDisabled, supportedMedia },
  onClick,
}) => {
  const [isChecked, setIsChecked] = useState(initialValue)

  const toggleValue = (event) => {
    event.preventDefault()
    if (isDisabled) return
    setIsChecked(!isChecked)
    onClick(!isChecked)
  }

  return (
    <tr
      css={{ cursor: isDisabled ? 'not-allowed' : 'pointer' }}
      onClick={toggleValue}
    >
      <td>
        <label
          css={{
            // prevents text highlight on double click
            userSelect: 'none', // Chrome
            WebkitUserSelect: 'none', // Safari
            MozUserSelect: 'none', // Firefox
          }}
        >
          <CheckboxIcon
            size={20}
            value={value}
            isChecked={isChecked}
            isDisabled={isDisabled}
            onClick={toggleValue}
          />
          <div className="hidden">
            {value}: {label}
          </div>
        </label>
      </td>
      <td>{value}</td>
      <td>{label}</td>
      <td>
        <div css={{ display: 'flex' }}>
          <ImagesSvg
            height={constants.icons.comfy}
            css={{ marginRight: spacing.normal }}
            color={
              supportedMedia?.includes('Images')
                ? colors.signal.canary.base
                : colors.structure.transparent
            }
          />
          <DocumentsSvg
            height={constants.icons.comfy}
            css={{ marginRight: spacing.normal }}
            color={
              supportedMedia?.includes('Documents')
                ? colors.graph.seafoam
                : colors.structure.transparent
            }
          />
          <VideosSvg
            height={constants.icons.comfy}
            color={
              supportedMedia?.includes('Videos')
                ? colors.graph.iris
                : colors.structure.transparent
            }
          />
        </div>
      </td>
    </tr>
  )
}

CheckboxTableRow.propTypes = {
  option: PropTypes.shape(checkboxOptionShape).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default CheckboxTableRow
