import { useState } from 'react'
import PropTypes from 'prop-types'
import Router from 'next/router'

import { fetcher, getQueryString } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonActions from '../Button/Actions'
import Modal from '../Modal'

const OrganizationOwnersMenu = ({ organizationId, ownerId, revalidate }) => {
  const [isRemoveModalOpen, setRemoveModalOpen] = useState(false)
  const [isRemoving, setIsRemoving] = useState(false)

  return (
    <>
      <Menu open="bottom-left" button={ButtonActions}>
        {({ onBlur, onClick }) => (
          <div>
            <ul>
              <li>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={() => {
                    onClick()

                    setRemoveModalOpen(true)
                  }}
                >
                  Remove Owner
                </Button>
              </li>
            </ul>
          </div>
        )}
      </Menu>

      {isRemoveModalOpen && (
        <Modal
          title="Remove Owner from Organization"
          message="This owner will be removed from the organization admin, but will retain access to any projects previously added to."
          action={isRemoving ? 'Removing...' : 'Remove Owner'}
          onCancel={() => {
            setRemoveModalOpen(false)
          }}
          onConfirm={async () => {
            setIsRemoving(true)

            await fetcher(
              `/api/v1/organizations/${organizationId}/owners/${ownerId}/`,
              {
                method: 'DELETE',
              },
            )

            setRemoveModalOpen(false)

            revalidate()

            const queryString = getQueryString({
              action: 'remove-owner-success',
            })

            Router.push(
              `/organizations/[organizationId]/owners${queryString}`,
              `/organizations/${organizationId}/owners`,
            )
          }}
        />
      )}
    </>
  )
}

OrganizationOwnersMenu.propTypes = {
  organizationId: PropTypes.string.isRequired,
  ownerId: PropTypes.number.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default OrganizationOwnersMenu
