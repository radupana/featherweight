package com.github.radupana.featherweight.viewmodel

import com.github.radupana.featherweight.service.AIProgrammeResponse

/**
 * Simple singleton to hold the most recently generated programme
 * for passing between generator and preview screens.
 * In a production app, this would be handled through proper navigation arguments
 * or a shared ViewModel with proper lifecycle management.
 */
object GeneratedProgrammeHolder {
    private var _currentResponse: AIProgrammeResponse? = null
    
    fun setGeneratedProgramme(response: AIProgrammeResponse) {
        _currentResponse = response
    }
    
    fun getGeneratedProgramme(): AIProgrammeResponse? {
        return _currentResponse
    }
    
    fun clearGeneratedProgramme() {
        _currentResponse = null
    }
}